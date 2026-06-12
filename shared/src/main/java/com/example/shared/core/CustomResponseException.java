package com.example.shared.core;

import lombok.Getter;

@Getter
public class CustomResponseException extends RuntimeException {
    private final int code;

    public CustomResponseException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static CustomResponseException ResourceNotFound(String message) {
        return new CustomResponseException(404, message);
    }

    public static CustomResponseException BadRequest(String message) {
        return new CustomResponseException(400, message);
    }

    public static CustomResponseException TooManyRequest() {
        return new CustomResponseException(429, "Too many requests");
    }

    public static CustomResponseException BadCredential() {
        return new CustomResponseException(400, "Bad Credentials");
    }

    public static CustomResponseException InternalServerError(String message) {
        return new CustomResponseException(500, message);
    }
}
