package com.kengchacha.common;

/**
 * 统一响应体。code=0 表示成功。
 */
public record ApiResponse<T>(int code, String msg, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "ok", null);
    }

    public static <T> ApiResponse<T> fail(String msg) {
        return new ApiResponse<>(1, msg, null);
    }
}
