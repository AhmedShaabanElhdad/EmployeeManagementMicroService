package com.example.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.example.shared.core.CustomResponseException;
import com.example.shared.core.GlobalResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
@RequiredArgsConstructor
@Slf4j
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";

        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode statusCode = rse.getStatusCode();
            status = HttpStatus.resolve(statusCode.value());
            if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
            message = "Access Denied: " + ex.getMessage();
        } else if (ex instanceof AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Authentication Failed: " + ex.getMessage();
        } else if (ex instanceof SecurityException) {
            status = HttpStatus.UNAUTHORIZED;
            message = "Security Error: " + ex.getMessage();
        } else if (isConnectionError(ex)) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
            message = "Service is temporarily unavailable. Please try again later.";
        } else if (isTimeoutError(ex)) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            message = "Request timed out while waiting for downstream service.";
        } else if (ex instanceof CustomResponseException cre) {
            status = HttpStatus.resolve(cre.getCode());
            if (status == null) {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
            }
            message = cre.getMessage();
        } else if (ex.getMessage() != null) {
            message = ex.getMessage();
        }

        // Improved logging:
        // Log 5xx as ERROR (with stack trace for true internal errors)
        // Log 4xx as WARN
        if (status.is5xxServerError()) {
            if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Gateway 500 Error: {} - Path: {}", message, exchange.getRequest().getPath(), ex);
            } else {
                log.error("Gateway Server Error ({}): {} - Path: {}", status.value(), message, exchange.getRequest().getPath());
            }
        } else {
            log.warn("Gateway Client Error ({}): {} - Path: {}", status.value(), message, exchange.getRequest().getPath());
        }

        try {
            response.setStatusCode(status);
        } catch (UnsupportedOperationException e) {
            log.warn("Could not set status code on response: {}", e.getMessage());
        }

        GlobalResponse<Void> globalResponse = GlobalResponse.failure(status.value(), message);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(globalResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            log.error("Error writing JSON response", e);
            return Mono.error(e);
        }
    }

    private boolean isConnectionError(Throwable ex) {
        return ex instanceof ConnectException || 
               (ex.getCause() != null && ex.getCause() instanceof ConnectException) ||
               ex.getClass().getName().contains("ConnectException");
    }

    private boolean isTimeoutError(Throwable ex) {
        return ex instanceof TimeoutException || 
               (ex.getCause() != null && ex.getCause() instanceof TimeoutException) ||
               ex.getClass().getName().contains("TimeoutException");
    }
}
