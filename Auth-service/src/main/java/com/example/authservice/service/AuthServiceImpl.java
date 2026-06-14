package com.example.authservice.service;

import com.example.authservice.abstraction.AuthService;
import com.example.authservice.client.EmployeeClient;
import com.example.authservice.dtos.AuthResponseDTO;
import com.example.authservice.dtos.EmployeeResponse;
import com.example.authservice.dtos.LoginRequestDTO;
import com.example.authservice.dtos.RefreshTokenRequestDTO;
import com.example.authservice.dtos.SignUpRequestDTO;
import com.example.authservice.dtos.UserIdRequestDTO;
import com.example.authservice.dtos.UserResponseDTO;
import com.example.authservice.entity.AuthOutbox;
import com.example.authservice.entity.Token;
import com.example.authservice.entity.UserAccount;
import com.example.authservice.helper.JwtHelper;
import com.example.authservice.mapper.Mapper;
import com.example.authservice.repo.AuthOutboxRepo;
import com.example.authservice.repo.TokenRepo;
import com.example.authservice.repo.UserAccountRepo;
import com.example.shared.core.CustomResponseException;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmployeeClient employeeClient;
    private final UserAccountRepo userAccountRepo;
    private final AuthOutboxRepo outboxRepo;
    private final TokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtHelper jwtHelper;
    private final MetricsProvider metricsProvider;
    private final ObjectMapper objectMapper;
    private final LoginAttemptService loginAttemptService;

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#signUpRequestDTO.username()")
    public UserResponseDTO signup(SignUpRequestDTO signUpRequestDTO, String token) {
        if (metricsProvider != null) metricsProvider.incrementCounter("auth.signup.request");

        EmployeeResponse employee;
        try {
            employee = employeeClient.getEmployeeByToken(token);
        } catch (FeignException.NotFound ex) {
            throw CustomResponseException.ResourceNotFound("Employee not found");
        }

        if (employee.verified()) {
            if (metricsProvider != null)
                metricsProvider.incrementCounter("auth.signup.error", "reason", "already_verified");
            throw CustomResponseException.BadRequest("Account Already Verified");
        }

        if (userAccountRepo.findByUsername(signUpRequestDTO.username()).isPresent()) {
            if (metricsProvider != null)
                metricsProvider.incrementCounter("auth.signup.error", "reason", "user_exists");
            throw CustomResponseException.BadRequest("Username already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(signUpRequestDTO.username());
        userAccount.setPassword(passwordEncoder.encode(signUpRequestDTO.password()));
        userAccount.setEmployeeId(employee.employeeId());
        userAccount.setRole(UserAccount.ROLE.USER);

        userAccount = userAccountRepo.save(userAccount);
        loginAttemptService.audit(userAccount.getUsername(), "SIGNUP", "User registered successfully");

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
            throw CustomResponseException.InternalServerError("Failed to initiate verification");
        }

        if (metricsProvider != null) metricsProvider.incrementCounter("auth.signup.success");
        return Mapper.toUserResponseDTO(userAccount);
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        long startTime = System.currentTimeMillis();
        if (metricsProvider != null) metricsProvider.incrementCounter("auth.login.request");

        // Use the service to handle locking checks in a separate transaction.
        // This avoids transaction rollback issues if authentication fails later.
        UserAccount userAccount = loginAttemptService.processPreLogin(loginRequestDTO.username());

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequestDTO.username(),
                    loginRequestDTO.password()
            ));

            loginAttemptService.recordSuccess(loginRequestDTO.username());
            AuthResponseDTO response = generateAuthResponse(userAccount);

            // isolated txn: reset attempts, audit, revoke old tokens, save new token

            revokeAllUserTokens(userAccount);
            saveUserToken(userAccount, response.accessToken());

            loginAttemptService.audit(userAccount.getUsername(), "LOGIN", "Login successful");
            if (metricsProvider != null) {
                metricsProvider.recordExecutionTime("auth.login.time", System.currentTimeMillis() - startTime);
                metricsProvider.incrementCounter("auth.login.success");
            }
            return response;

        } catch (AuthenticationException e) {
            // FIX: Use external service to ensure a NEW transaction is used for these updates.
            // This prevents a TransactionSystemException from hiding the 401 error.
            loginAttemptService.recordFailedAttempt(loginRequestDTO.username());
            loginAttemptService.audit(loginRequestDTO.username(), "FAILED_LOGIN", "Invalid credentials");
            if (metricsProvider != null)
                metricsProvider.incrementCounter("auth.login.error", "reason", "bad_credentials");
            throw e;
        }
    }

    @Override
    @Transactional
    public AuthResponseDTO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        String refreshToken = refreshTokenRequestDTO.refreshToken();
        String username = jwtHelper.extractUsername(refreshToken);

        UserAccount userAccount = userAccountRepo.findByUsernameWithLock(username)
                .orElseThrow(CustomResponseException::BadCredential);

        if (!jwtHelper.isRefreshTokenValid(refreshToken, userAccount)) {
            throw CustomResponseException.BadCredential();
        }

        AuthResponseDTO response = generateAuthResponse(userAccount);
        revokeAllUserTokens(userAccount);
        saveUserToken(userAccount, response.accessToken());

        return response;
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "auth_responses", allEntries = true),
            @CacheEvict(value = "users", allEntries = true)
    })
    public void logout(String token) {
        tokenRepo.findByToken(token).ifPresent(storedToken -> {
            storedToken.setExpired(true);
            storedToken.setRevoked(true);
            tokenRepo.save(storedToken);
        });
    }

    private void saveUserToken(UserAccount userAccount, String jwtToken) {
        tokenRepo.save(Token.builder()
                .user(userAccount)
                .token(jwtToken)
                .type(Token.TokenType.BEARER)
                .expired(false)
                .revoked(false)
                .build());
    }

    private void revokeAllUserTokens(UserAccount user) {
        var validUserTokens = tokenRepo.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) return;
        validUserTokens.forEach(token -> {
            token.setExpired(true);
            token.setRevoked(true);
        });
        tokenRepo.saveAll(validUserTokens);
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
