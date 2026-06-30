package com.ratelimiter.core.store;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.strategy.RateLimiter;

public class InMemoryRateLimitStore implements RateLimitStore {

    private final Map<String, RateLimiter> limitersByRuleName = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult allow(String scopeKey, RateLimitRule rule) {
        RateLimiter limiter = limitersByRuleName.computeIfAbsent(
                rule.getName(),
                name -> RateLimiterFactory.create(rule));
        return limiter.allow(scopeKey);
    }
}
