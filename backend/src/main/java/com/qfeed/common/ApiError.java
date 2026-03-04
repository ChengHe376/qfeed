package com.qfeed.common;

public class ApiError {
    public String code;
    public String message;

    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
