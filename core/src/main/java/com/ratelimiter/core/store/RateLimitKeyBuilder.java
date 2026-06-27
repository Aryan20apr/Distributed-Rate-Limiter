package com.ratelimiter.core.store;

import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.identity.ClientIdentity;

public final class RateLimitKeyBuilder {

    private static final String REDIS_PREFIX = "rl:";

    private RateLimitKeyBuilder() {}

    public static String scopeKey(RateLimitRule rule, ClientIdentity identity, String endpoint) {
        return switch (rule.getScope()) {
            case GLOBAL -> "global";
            case USER -> "user:" + identity.userId();
            case IP -> "ip:" + identity.ipAddress();
            case API_KEY -> {
                if (identity.apiKey() == null) {
                    yield "api-key:unauthenticated";
                }
                yield "api-key:" + identity.apiKey();
            }
            case ENDPOINT -> "endpoint:" + endpoint;
            case USER_ENDPOINT -> "user:" + identity.userId() + ":endpoint:" + endpoint;
        };
    }

    public static String redisKey(String ruleName, String scopeKey) {
        return REDIS_PREFIX + ruleName + ":" + scopeKey;
    }
}