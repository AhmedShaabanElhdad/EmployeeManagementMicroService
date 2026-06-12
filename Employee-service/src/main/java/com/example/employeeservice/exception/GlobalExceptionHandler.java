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
        var errors = List.of(new GlobalResponse.ErrorItem(ex.getMessage()));
        return new ResponseEntity<>(new GlobalResponse<>(ex.getCode(), errors), HttpStatus.valueOf(ex.getCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getAllErrors().stream().map(error ->
                new GlobalResponse.ErrorItem(error.getObjectName() + " : " + error.getDefaultMessage())
        ).toList();
        return new ResponseEntity<>(new GlobalResponse<>(400, errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GlobalResponse<?>> handleMaxSizeException(MaxUploadSizeExceededException exc) {
        var errors = List.of(new GlobalResponse.ErrorItem("File too large! Maximum upload size is 10MB."));
        return new ResponseEntity<>(new GlobalResponse<>(413, errors), HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GlobalResponse<?>> handleFeignException(FeignException ex) {
        log.error("Feign communication error", ex);
        var errors = List.of(new GlobalResponse.ErrorItem("Service communication error"));
        int status = ex.status() != -1 ? ex.status() : 503;
        return new ResponseEntity<>(new GlobalResponse<>(status, errors), HttpStatus.valueOf(status));
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<GlobalResponse<?>> handleTimeoutException(TimeoutException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem("Request timed out."));
        return new ResponseEntity<>(new GlobalResponse<>(408, errors), HttpStatus.REQUEST_TIMEOUT);
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<GlobalResponse<?>> handleCircuitBreakerException(CallNotPermittedException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem("Service is temporarily unavailable (Circuit Breaker open)."));
        return new ResponseEntity<>(new GlobalResponse<>(503, errors), HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        String message = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "An unexpected error occurred";
        var errors = List.of(new GlobalResponse.ErrorItem(message));
        return new ResponseEntity<>(new GlobalResponse<>(500, errors), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
