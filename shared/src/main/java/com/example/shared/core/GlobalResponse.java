package com.example.shared.core;

import lombok.Getter;
import java.util.List;

@Getter
public class GlobalResponse<T> {
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    public int code;
    public String status;
    public T data;
    public List<ErrorItem> errorItems;

    public GlobalResponse(int code, List<ErrorItem> errorItems) {
        this.code = code;
        this.errorItems = errorItems;
        this.status = FAILURE;
        this.data = null;
    }

    public GlobalResponse(T data) {
        this.code = 200;
        this.errorItems = null;
        this.status = SUCCESS;
        this.data = data;
    }

    public record ErrorItem(String message) {}
}
