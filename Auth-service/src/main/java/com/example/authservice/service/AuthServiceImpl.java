package com.example.authservice.service;

import com.example.authservice.abstraction.AuthService;
import com.example.authservice.client.EmployeeClient;
import com.example.authservice.config.KafkaProducerConfig;
import com.example.authservice.dtos.*;
import com.example.authservice.entity.AuthOutbox;
import com.example.authservice.entity.UserAccount;
import com.example.authservice.helper.JwtHelper;
import com.example.authservice.mapper.Mapper;
import com.example.authservice.repo.AuthOutboxRepo;
import com.example.authservice.repo.UserAccountRepo;
import com.example.shared.monitoring.MetricsProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.CustomResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final EmployeeClient employeeClient;
    private final UserAccountRepo userAccountRepo;
    private final AuthOutboxRepo outboxRepo;
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

        if (userAccountRepo.findByUserName(signUpRequestDTO.username()).isPresent()) {
            metricsProvider.incrementCounter("auth.signup.error", "reason", "user_exists");
            throw CustomResponseException.BadRequest("Username already exists");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(signUpRequestDTO.username());
        userAccount.setPassword(passwordEncoder.encode(signUpRequestDTO.password()));
        userAccount.setEmployeeId(employee.employeeId());
        
        userAccountRepo.save(userAccount);

        log.info("User created successfully: {}", signUpRequestDTO.username());

        // FIX: Transactional Outbox Pattern to solve Dual Write bug
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
            throw new RuntimeException("Internal Server Error during signup");
        }

        metricsProvider.incrementCounter("auth.signup.success");
        return Mapper.toUserResponseDTO(userAccount);
    }

    @Override
    @Cacheable(value = "auth_responses", key = "#loginRequestDTO.username()", unless = "#result == null")
    public AuthResponseDTO login(LoginRequestDTO loginRequestDTO) {
        // CPU Protection: Caching this response prevents repeated expensive BCrypt operations
        long startTime = System.currentTimeMillis();
        metricsProvider.incrementCounter("auth.login.request");
        log.info("Authenticating user: {}", loginRequestDTO.username());
        
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    loginRequestDTO.username(),
                    loginRequestDTO.password()
            ));
        } catch (Exception e) {
            metricsProvider.incrementCounter("auth.login.error", "reason", "bad_credentials");
            throw e;
        }

        UserAccount userAccount = userAccountRepo.findByUserName(loginRequestDTO.username())
                .orElseThrow(() -> {
                    metricsProvider.incrementCounter("auth.login.error", "reason", "user_not_found");
                    return CustomResponseException.BadCredential();
                });

        AuthResponseDTO response = generateAuthResponse(userAccount);
        metricsProvider.recordExecutionTime("auth.login.time", System.currentTimeMillis() - startTime);
        metricsProvider.incrementCounter("auth.login.success");
        return response;
    }

    @Override
    public AuthResponseDTO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        metricsProvider.incrementCounter("auth.refresh.request");
        String refreshToken = refreshTokenRequestDTO.refreshToken();
        String username = jwtHelper.extractUsername(refreshToken);
        
        UserAccount userAccount = userAccountRepo.findByUserName(username)
                .orElseThrow(CustomResponseException::BadCredential);

        if (!jwtHelper.isRefreshTokenValid(refreshToken, userAccount)) {
            metricsProvider.incrementCounter("auth.refresh.error", "reason", "invalid_token");
            throw CustomResponseException.BadCredential();
        }

        metricsProvider.incrementCounter("auth.refresh.success");
        return generateAuthResponse(userAccount);
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
        }
        
        return claims;
    }
}
