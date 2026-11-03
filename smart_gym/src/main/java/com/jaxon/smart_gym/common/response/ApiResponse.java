package com.jaxon.smart_gym.common.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId,
        Instant timestamp
) {

    private static final String SUCCESS_CODE = "OK";
    private static final String SUCCESS_MESSAGE = "success";

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(
                true,
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                data,
                traceId,
                Instant.now()
        );
    }

    public static ApiResponse<Void> success(String traceId) {
        return new ApiResponse<>(
                true,
                SUCCESS_CODE,
                SUCCESS_MESSAGE,
                null,
                traceId,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> success(
            String message,
            T data,
            String traceId
    ) {
        return new ApiResponse<>(
                true,
                SUCCESS_CODE,
                message,
                data,
                traceId,
                Instant.now()
        );
    }

    public static ApiResponse<Void> failure(
            String code,
            String message,
            String traceId
    ) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null,
                traceId,
                Instant.now()
        );
    }

    public static <T> ApiResponse<T> failure(
            String code,
            String message,
            T data,
            String traceId
    ) {
        return new ApiResponse<>(
                false,
                code,
                message,
                data,
                traceId,
                Instant.now()
        );
    }
}