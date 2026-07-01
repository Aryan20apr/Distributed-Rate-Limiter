package com.ratelimiter.core.utils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.store.RateLimiterFactory;

public final class RuleValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9-]{1,63}$");

    private RuleValidator() {}

    public static void validateForRedis(RateLimitRule rule) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateCommon(rule, errors);
        if (rule.getAlgorithm() != null
                && !RateLimiterFactory.isRedisSupported(rule.getAlgorithm())) {
            errors.put("algorithm",
                    "Algorithm '" + rule.getAlgorithm()
                    + "' is not supported with rate-limit.store=redis. "
                    + "Use 'token' or 'sliding-counter'.");
        }
        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    public static void validateForMemory(RateLimitRule rule) {
        Map<String, String> errors = new LinkedHashMap<>();
        validateCommon(rule, errors);
        if (!errors.isEmpty()) {
            throw new RuleValidationException(errors);
        }
    }

    private static void validateCommon(RateLimitRule rule, Map<String, String> errors) {
        if (rule.getName() == null || rule.getName().isBlank()) {
            errors.put("name", "name is required");
        } else if (!NAME_PATTERN.matcher(rule.getName()).matches()) {
            errors.put("name", "name must be lowercase alphanumeric with hyphens, 2-64 chars");
        }
        if (rule.getScope() == null) {
            errors.put("scope", "scope is required");
        }
        if (rule.getAlgorithm() == null || rule.getAlgorithm().isBlank()) {
            errors.put("algorithm", "algorithm is required");
        }
        if (rule.getPriority() < 0) {
            errors.put("priority", "priority must be >= 0");
        }
        if (rule.getScope() == RateLimitScope.API_KEY
                && rule.getEndpointPattern() == null) {
            // optional warning only — API_KEY without pattern applies everywhere
        }
        String algo = rule.getAlgorithm() == null ? "" : rule.getAlgorithm().toLowerCase();
        switch (algo) {
            case "token", "leaky" -> {
                if (rule.getCapacity() <= 0) {
                    errors.put("capacity", "capacity must be > 0 for token/leaky algorithms");
                }
                if (rule.getRefillPerSecond() <= 0) {
                    errors.put("refillPerSecond", "refillPerSecond must be > 0");
                }
            }
            case "fixed", "sliding-log", "sliding-counter" -> {
                if (rule.getMaxRequests() <= 0) {
                    errors.put("maxRequests", "maxRequests must be > 0");
                }
                if (rule.getWindowMillis() <= 0) {
                    errors.put("windowMillis", "windowMillis must be > 0");
                }
            }
            case "" -> { /* already reported */ }
            default -> errors.put("algorithm", "unknown algorithm: " + rule.getAlgorithm());
        }
    }
}