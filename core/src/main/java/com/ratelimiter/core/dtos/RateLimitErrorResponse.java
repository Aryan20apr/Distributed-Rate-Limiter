package com.ratelimiter.core.dtos;

public record RateLimitErrorResponse(
    String error,
    String message,
    long retryAfterSeconds
) {
public static RateLimitErrorResponse exceeded(String ruleName, long retryAfterSeconds) {
    return new RateLimitErrorResponse(
            "rate_limit_exceeded",
            "Rate limit exceeded for rule '" + ruleName + "'.",
            retryAfterSeconds);
}
}
