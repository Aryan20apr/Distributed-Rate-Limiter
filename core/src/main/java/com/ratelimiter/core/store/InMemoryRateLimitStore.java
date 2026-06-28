package com.ratelimiter.core.store;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.strategy.RateLimiter;

public class InMemoryRateLimitStore implements RateLimitStore {

    private final Map<String, RateLimiter> limitersByRuleName = new ConcurrentHashMap<>();

    public InMemoryRateLimitStore(List<RateLimitRule> rules) {
        for (RateLimitRule rule : rules) {
            limitersByRuleName.put(rule.getName(), RateLimiterFactory.create(rule));
        }
    }

    @Override
    public RateLimitResult allow(String scopeKey, RateLimitRule rule) {
        RateLimiter limiter = limitersByRuleName.get(rule.getName());
        if (limiter == null) {
            throw new IllegalStateException("No limiter registered for rule: " + rule.getName());
        }
        return limiter.allow(scopeKey);
    }
}