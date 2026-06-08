package com.example.authservice.config;

import com.example.authservice.helper.JwtHelper;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {

    private final JwtHelper jwtHelper;

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            String token = jwtHelper.generateInternalServiceToken("auth-service");
            template.header("Authorization", "Bearer " + token);
        };
    }
}
