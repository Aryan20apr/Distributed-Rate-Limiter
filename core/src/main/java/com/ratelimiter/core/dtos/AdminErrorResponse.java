package com.ratelimiter.core.dtos;

import java.util.Map;

public record AdminErrorResponse(
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static AdminErrorResponse of(String error, String message) {
        return new AdminErrorResponse(error, message, null);
    }

    public static AdminErrorResponse validation(Map<String, String> fieldErrors) {
        return new AdminErrorResponse("validation_failed", "Request validation failed", fieldErrors);
    }
}