package com.example.authservice.service;

import com.example.authservice.abstraction.AuthService;
import com.example.authservice.client.EmployeeClient;
import com.example.authservice.dtos.*;
import com.example.authservice.entity.AuditLog;
import com.example.authservice.entity.AuthOutbox;
import com.example.authservice.entity.Token;
import com.example.authservice.entity.UserAccount;
import com.example.authservice.helper.JwtHelper;
import com.example.authservice.mapper.Mapper;
import com.example.authservice.repo.AuditLogRepo;
import com.example.authservice.repo.AuthOutboxRepo;
import com.example.authservice.repo.TokenRepo;
import com.example.authservice.repo.UserAccountRepo;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.CustomResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_TIME_DURATION = 15; // 15 minutes

    private final EmployeeClient employeeClient;
    private final UserAccountRepo userAccountRepo;
    private final AuthOutboxRepo outboxRepo;
    private final TokenRepo tokenRepo;
    private final AuditLogRepo auditLogRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final MetricsProvider metricsProvider;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#signUpRequestDTO.username()")
    public UserResponseDTO signup(SignUpRequestDTO signUpRequestDTO, String token) {
        metricsProvider.incrementCounter("auth.signup.request");
        EmployeeResponse employee = employeeClient.getEmployeeByToken(token);

        if (employee.verified()) {
            metricsProvider.incrementCounter("auth.signup.error", "reason", "already_verified");
            throw CustomResponseException.BadRequest("Account Already Verified");
        }

        if (userAccountRepo.findByUsername(signUpRequestDTO.username()).isPresent()) {
            metricsProvider.incrementCounter("auth.signup.error", "reason", "user_exists");
            throw CustomResponseException.BadRequest("Username already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(signUpRequestDTO.username());
        userAccount.setPassword(passwordEncoder.encode(signUpRequestDTO.password()));
        userAccount.setEmployeeId(employee.employeeId());
        userAccount.setRole(UserAccount.ROLE.USER);

        userAccountRepo.save(userAccount);

        auditLog(userAccount.getUsername(), "SIGNUP", "User registered successfully");
        log.info("User created successfully: {}", signUpRequestDTO.username());

        UserIdRequestDTO verificationEvent = new UserIdRequestDTO(userAccount.getEmployeeId().toString());
        try {
            outboxRepo.save(AuthOutbox.builder()
                    .aggregateId(userAccount.getId().toString())
                    .eventType("EmployeeVerificationInitiated")
                    .payload(objectMapper.writeValueAsString(verificationEvent))
                    .createdAt(Instant.now())
                    .processed(false)
                    .build());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize verification event", e);
            throw CustomResponseException.InternalServerError("Internal Server Error during signup");
        }

        metricsProvider.incrementCounter("auth.signup.success");
        return Mapper.toUserResponseDTO(userAccount);
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        long startTime = System.currentTimeMillis();
        metricsProvider.incrementCounter("auth.login.request");
        log.info("Authenticating user: {}", loginRequestDTO.username());

        // Lock the user record to manage failed attempts and sessions safely
        UserAccount userAccount = userAccountRepo.findByUsernameWithLock(loginRequestDTO.username())
                .orElseThrow(() -> {
                    metricsProvider.incrementCounter("auth.login.error", "reason", "user_not_found");
                    return CustomResponseException.BadCredential();
                });

        if (userAccount.isAccountLocked()) {
            if (unlockWhenTimeExpired(userAccount)) {
                log.info("Account unlocked for user: {}", userAccount.getUsername());
            } else {
                metricsProvider.incrementCounter("auth.login.error", "reason", "account_locked");
                throw new LockedException("Account is locked. Try again in " + LOCK_TIME_DURATION + " minutes.");
            }
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequestDTO.username(),
                    loginRequestDTO.password()
            ));
            
            resetFailedAttempts(userAccount);
            AuthResponseDTO response = generateAuthResponse(userAccount);
            
            revokeAllUserTokens(userAccount);
            saveUserToken(userAccount, response.accessToken());
            
            auditLog(userAccount.getUsername(), "LOGIN", "Login successful");
            metricsProvider.recordExecutionTime("auth.login.time", System.currentTimeMillis() - startTime);
            metricsProvider.incrementCounter("auth.login.success");
            return response;

        } catch (Exception e) {
            increaseFailedAttempts(userAccount);
            auditLog(loginRequestDTO.username(), "FAILED_LOGIN", "Invalid credentials. Attempt " + userAccount.getFailedAttempts());
            metricsProvider.incrementCounter("auth.login.error", "reason", "bad_credentials");
            throw e;
        }
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        metricsProvider.incrementCounter("auth.refresh.request");
        String refreshToken = refreshTokenRequestDTO.refreshToken();
        String username = jwtHelper.extractUsername(refreshToken);

        // Lock the user record during refresh to prevent concurrent session rotations
        UserAccount userAccount = userAccountRepo.findByUsernameWithLock(username)
                .orElseThrow(CustomResponseException::BadCredential);

        if (!jwtHelper.isRefreshTokenValid(refreshToken, userAccount)) {
            metricsProvider.incrementCounter("auth.refresh.error", "reason", "invalid_token");
            throw CustomResponseException.BadCredential();
        }

        AuthResponseDTO response = generateAuthResponse(userAccount);
        revokeAllUserTokens(userAccount);
        saveUserToken(userAccount, response.accessToken());
        
        auditLog(username, "REFRESH", "Token refreshed");
        metricsProvider.incrementCounter("auth.refresh.success");
        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "auth_responses", allEntries = true),
        @CacheEvict(value = "users", allEntries = true)
    })
    public void logout(String token) {
        var storedToken = tokenRepo.findByToken(token)
                .orElse(null);
        if (storedToken != null) {
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            tokenRepo.save(storedToken);
            auditLog(storedToken.getUser().getUsername(), "LOGOUT", "Logout successful");
        }
        metricsProvider.incrementCounter("auth.logout.success");
    }

    private void saveUserToken(UserAccount userAccount, String jwtToken) {
        var token = Token.builder()
                .user(userAccount)
                .token(jwtToken)
                .type(Token.TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepo.save(token);
    }

    private void revokeAllUserTokens(UserAccount user) {
        var validUserTokens = tokenRepo.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepo.saveAll(validUserTokens);
    }

    private void auditLog(String username, String action, String details) {
        auditLogRepo.save(AuditLog.builder()
                .username(username)
                .action(action)
                .details(details)
                .timestamp(Instant.now())
                .build());
    }

    private void increaseFailedAttempts(UserAccount user) {
        int newFailAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailAttempts);
        if (newFailAttempts >= MAX_FAILED_ATTEMPTS) {
            user.setAccountLocked(true);
            user.setLockTime(LocalDateTime.now());
            auditLog(user.getUsername(), "LOCKOUT", "Account locked due to 5 failed attempts");
        }
        userAccountRepo.save(user);
    }

    private void resetFailedAttempts(UserAccount user) {
        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLockTime(null);
            userAccountRepo.save(user);
        }
    }

    private boolean unlockWhenTimeExpired(UserAccount user) {
        if (user.getLockTime() != null && 
            user.getLockTime().plusMinutes(LOCK_TIME_DURATION).isBefore(LocalDateTime.now())) {
            user.setAccountLocked(false);
            user.setLockTime(null);
            user.setFailedAttempts(0);
            userAccountRepo.save(user);
            return true;
        }
        return false;
    }

    private AuthResponseDTO generateAuthResponse(UserAccount userAccount) {
        String accessToken = jwtHelper.generateAccessToken(createClaims(userAccount, "access"), userAccount);
        String refreshToken = jwtHelper.generateRefreshToken(createClaims(userAccount, "refresh"), userAccount);

        return new AuthResponseDTO(accessToken, refreshToken);
    }

    private Map<String, Object> createClaims(UserAccount user, String type) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", type);

        if ("access".equals(type)) {
            claims.put("role", user.getRole());
            claims.put("employeeId", user.getEmployeeId());
            claims.put("jti", UUID.randomUUID());
        }

        return claims;
    }
}
