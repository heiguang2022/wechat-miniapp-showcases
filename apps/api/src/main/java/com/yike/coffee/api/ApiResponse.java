package com.yike.coffee.api;

import jakarta.servlet.http.HttpServletRequest;

public record ApiResponse<T>(String code, String message, T data, String requestId) {
    public static <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return new ApiResponse<>("OK", "success", data, request.getAttribute("requestId").toString());
    }
}
