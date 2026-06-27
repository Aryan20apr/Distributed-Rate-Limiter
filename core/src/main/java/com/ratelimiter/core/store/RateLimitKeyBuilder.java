package com.ratelimiter.core.store;

import com.ratelimiter.core.dtos.RateLimitRule;

public class RateLimitKeyBuilder {
    
    private static final String REDIS_PREFIX = "rl:";

    private RateLimitKeyBuilder() {}

    public static String scopeKey(RateLimitRule rule, String user, String endpoint) {
        return switch (rule.getScope()) {
            case GLOBAL -> "global";
            case USER -> "user:" + user;
            case ENDPOINT -> "endpoint:" + endpoint;
            case USER_ENDPOINT -> "user:" + user + ":endpoint:" + endpoint;
        };
    }

    public static String redisKey(String ruleName, String scopeKey) {
        return REDIS_PREFIX + ruleName + ":" + scopeKey;
    }

}
