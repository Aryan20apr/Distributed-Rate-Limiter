package com.ratelimiter.core.utils;

import com.ratelimiter.core.dtos.RateLimitRule;

public final class RuleLimitResolver {

    private RuleLimitResolver() {}

    public static long resolveLimit(RateLimitRule rule) {
        return switch (rule.getAlgorithm().toLowerCase()) {
            case "token", "leaky" -> (long) rule.getCapacity();
            case "fixed", "sliding-log", "sliding-counter" -> rule.getMaxRequests();
            default -> throw new IllegalArgumentException(
                    "Unknown algorithm: " + rule.getAlgorithm());
        };
    }
}