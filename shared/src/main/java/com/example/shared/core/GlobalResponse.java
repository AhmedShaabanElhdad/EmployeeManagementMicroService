package com.example.shared.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class GlobalResponse<T> {
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    public int code;
    public String status;
    public T data;
    public List<ErrorItem> errorItems;

    // Constructor for Success
    public GlobalResponse(T data) {
        this.code = 200;
        this.status = SUCCESS;
        this.data = data;
        this.errorItems = null;
    }

    // Constructor for Failure
    public GlobalResponse(int code, List<ErrorItem> errorItems) {
        this.code = code;
        this.status = FAILURE;
        this.errorItems = errorItems;
        this.data = null;
    }

    public record ErrorItem(String message) {}

    // Static helper to avoid ambiguity
    public static <T> GlobalResponse<T> error(int code, String message) {
        return new GlobalResponse<>(code, List.of(new ErrorItem(message)));
    }
}
