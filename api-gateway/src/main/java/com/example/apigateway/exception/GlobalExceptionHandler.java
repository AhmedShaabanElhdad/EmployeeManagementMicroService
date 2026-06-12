package com.example.apigateway.exception;

import com.example.shared.core.CustomResponseException;
import com.example.shared.core.GlobalResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomResponseException.class)
    public ResponseEntity<GlobalResponse<?>> handleCustomResponseException(CustomResponseException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem(ex.getMessage()));
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.valueOf(ex.getCode()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<GlobalResponse<?>> handleResponseStatusException(ResponseStatusException ex) {
        var errors = List.of(new GlobalResponse.ErrorItem(ex.getReason() != null ? ex.getReason() : "An error occurred"));
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.valueOf(ex.getStatusCode().value()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleGeneralException(Exception ex) {
        var errors = List.of(new GlobalResponse.ErrorItem(ex.getMessage()));
        return new ResponseEntity<>(new GlobalResponse<>(errors), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
