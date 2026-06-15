package com.example.notification.exception;

import com.example.shared.core.CustomResponseException;
import com.example.shared.core.GlobalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        String message = (ex != null && ex.getMessage() != null) ? ex.getMessage() : "An unexpected error occurred";
        return new ResponseEntity<>(GlobalResponse.failure(500, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
