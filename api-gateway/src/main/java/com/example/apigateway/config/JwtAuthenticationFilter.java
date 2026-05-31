package com.example.apigateway.config;

import com.example.shared.monitoring.MetricsProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtHelper jwtHelper;
    private final MetricsProvider metricsProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        metricsProvider.incrementCounter("gateway.request.received", "path", path, "method", method);

        log.info("Incoming request {} {}", method, exchange.getRequest().getURI());
        Set<String> publicEndpoints = Set.of(
                "/api/v1/auth/login",
                "/api/v1/auth/signup",
                "/api/v1/auth/refresh"
        );

        if (publicEndpoints.contains(path))
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            metricsProvider.incrementCounter("gateway.auth.error", "reason", "missing_header");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        Claims claims = jwtHelper.validateToken(token);
        if (claims == null) {
            metricsProvider.incrementCounter("gateway.auth.error", "reason", "invalid_token");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String correlationId = UUID.randomUUID().toString();
        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", claims.get("userId", String.class))
                .header("X-Correlation-Id", correlationId)
                // todo check role tradeoff
                .header("X-Role", claims.get("role", String.class))
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
                    metricsProvider.recordExecutionTime("gateway.request.processing.time", 
                            System.currentTimeMillis() - startTime, "path", path, "status", status.name());
                    log.info("Response status: {}", status);
                }));
    }
}
