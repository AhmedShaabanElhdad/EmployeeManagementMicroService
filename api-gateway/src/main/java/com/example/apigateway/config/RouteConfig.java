package com.example.apigateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
@RequiredArgsConstructor
public class RouteConfig {

    private final RedisRateLimiter redisRateLimiter;
    private final KeyResolver userKeyResolver;

    @Bean
    public RouteLocator customRoutes(
            RouteLocatorBuilder builder
    ) {

        return builder.routes()

                .route("auth-service", route -> route
                        .path("/api/v1/auth/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("authServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/auth")
                                )
                        )
                        .uri("lb://auth-service")
                )

                .route("employee-service", route -> route
                        .path("/api/v1/employees/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("employeeServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/employees")
                                )
                                .retry(config -> config
                                        .setRetries(3)
                                        .setMethods(HttpMethod.GET)
                                )
                                .requestRateLimiter(config -> {
                                    config.setRateLimiter(redisRateLimiter);
                                    config.setKeyResolver(userKeyResolver);
                                })
                        )
                        .uri("lb://employee-service")
                )

                .route("department-service", route -> route
                        .path("/api/v1/departments/**")
                        .filters(f -> f
                                .circuitBreaker(config -> config
                                        .setName("departmentServiceCircuitBreaker")
                                        .setFallbackUri("forward:/fallback/department")
                                )
                        )
                        .uri("lb://department-service")
                )

                .build();
    }
}
