package com.example.employeeservice.exception;

import com.example.shared.core.CustomResponseException;
import com.example.shared.core.GlobalResponse;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomResponseException.class)
    public ResponseEntity<GlobalResponse<?>> handleCustomResponseException(CustomResponseException ex) {
        log.warn("Custom exception: {}", ex.getMessage());
        return new ResponseEntity<>(GlobalResponse.failure(ex.getCode(), ex.getMessage()), HttpStatus.valueOf(ex.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getAllErrors().stream().map(error ->
                new GlobalResponse.ErrorItem(error.getObjectName() + " : " + error.getDefaultMessage())
        ).toList();
        return new ResponseEntity<>(GlobalResponse.failure(400, errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GlobalResponse<?>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        return new ResponseEntity<>(GlobalResponse.failure(413, "File too large! Maximum upload size is 10MB."), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GlobalResponse<?>> handleFeignException(FeignException ex) {
        log.error("Feign communication error", ex);
        int status = ex.status() != -1 ? ex.status() : 503;
        return new ResponseEntity<>(GlobalResponse.failure(status, "Service communication error"), HttpStatus.valueOf(status));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<GlobalResponse<?>> handleTimeoutException(TimeoutException ex) {
        return new ResponseEntity<>(GlobalResponse.failure(408, "Request timed out."), HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<GlobalResponse<?>> handleCircuitBreakerException(CallNotPermittedException ex) {
        return new ResponseEntity<>(GlobalResponse.failure(503, "Service is temporarily unavailable (Circuit Breaker open)."), HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        String message = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "An unexpected error occurred";
        return new ResponseEntity<>(GlobalResponse.failure(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
