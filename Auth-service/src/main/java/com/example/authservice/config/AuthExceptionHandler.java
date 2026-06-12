package com.example.authservice.config;

import com.example.shared.core.GlobalResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<GlobalResponse<?>> handleBadCredentialsException(BadCredentialsException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem("Invalid username or password"));
        GlobalResponse<?> response = new GlobalResponse<>(errors);
        response.code = HttpStatus.UNAUTHORIZED.value();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GlobalResponse<?>> handleAuthenticationException(AuthenticationException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem(ex.getMessage()));
        GlobalResponse<?> response = new GlobalResponse<>(errors);
        response.code = HttpStatus.UNAUTHORIZED.value();
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
