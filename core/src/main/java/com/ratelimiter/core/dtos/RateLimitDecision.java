package com.ratelimiter.core.dtos;

public record RateLimitDecision(
    boolean allowed,
    long limit,
    long remaining,
    long resetAtEpochSeconds,
    long retryAfterMillis,
    String violatedRuleName
) {
public static RateLimitDecision allowed(long limit, long remaining, long resetAtEpochSeconds) {
    return new RateLimitDecision(true, limit, remaining, resetAtEpochSeconds, 0, null);
}

public static RateLimitDecision denied(
        String ruleName,
        long limit,
        long remaining,
        long resetAtEpochSeconds,
        long retryAfterMillis) {
    return new RateLimitDecision(
            false, limit, remaining, resetAtEpochSeconds, retryAfterMillis, ruleName);
}
}