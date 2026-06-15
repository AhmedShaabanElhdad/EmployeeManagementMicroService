package com.example.shared.core;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalResponse<T> {
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    public int code;
    public String status;
    public String message;
    public T data;
    public List<ErrorItem> errorItems;

    @Builder.Default
    public long timestamp = System.currentTimeMillis();

    public GlobalResponse(T data) {
        this.code = 200;
        this.status = SUCCESS;
        this.data = data;
        // timestamp is initialized by the field declaration or Builder.Default
    }

    public GlobalResponse(int code, List<ErrorItem> errorItems) {
        this.code = code;
        this.status = FAILURE;
        this.errorItems = errorItems;
        // timestamp is initialized by the field declaration or Builder.Default
    }

    // Static helper for success
    public static <T> GlobalResponse<T> success(T data) {
        return new GlobalResponse<>(data);
    }

    public static <T> GlobalResponse<T> success(T data, String message) {
        GlobalResponse<T> response = new GlobalResponse<>(data);
        response.setMessage(message);
        return response;
    }

    // Static helper for failure
    public static <T> GlobalResponse<T> failure(int code, String message) {
        GlobalResponse<T> response = new GlobalResponse<>(code, List.of(new ErrorItem(message)));
        response.setMessage(message);
        return response;
    }

    public static <T> GlobalResponse<T> failure(int code, List<ErrorItem> errorItems) {
        return new GlobalResponse<>(code, errorItems);
    }

    public record ErrorItem(String message) {
    }
}
