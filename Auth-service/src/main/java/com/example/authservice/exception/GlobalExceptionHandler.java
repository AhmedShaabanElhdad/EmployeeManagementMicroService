package com.example.authservice.exception;

import com.example.shared.core.CustomResponseException;
import com.example.shared.core.GlobalResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

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
        return new ResponseEntity<>(new GlobalResponse<>(400, errors), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<GlobalResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return new ResponseEntity<>(GlobalResponse.failure(401, "Invalid username or password"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<GlobalResponse<?>> handleInternalAuthException(InternalAuthenticationServiceException ex) {
        log.error("Internal Auth Error: ", ex);
        String msg = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
        return new ResponseEntity<>(GlobalResponse.failure(500, "Authentication service error: " + msg), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failure: {}", ex.getMessage());
        return new ResponseEntity<>(GlobalResponse.failure(401, ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        String message = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "An unexpected internal error occurred";
        return new ResponseEntity<>(GlobalResponse.failure(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
