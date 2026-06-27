package com.ratelimiter.core.dtos;

public record RateLimitResult(
    boolean allowed,
    long remaining,
    long retryAfterMillis,
    long limit,
    long resetAtEpochSeconds
) {
/** Legacy 3-arg constructor used by existing strategy classes until updated. */
public static RateLimitResult of(boolean allowed, long remaining, long retryAfterMillis) {
    long resetAt = retryAfterMillis > 0
            ? (System.currentTimeMillis() + retryAfterMillis) / 1000
            : 0;
    return new RateLimitResult(allowed, remaining, retryAfterMillis, 0, resetAt);
}
}
