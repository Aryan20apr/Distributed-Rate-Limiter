package com.ratelimiter.core.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.store.RateLimitKeyBuilder;
import com.ratelimiter.core.store.RateLimitStore;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RateLimitManager {

    private final RateLimitStore store;
    private final List<RateLimitRule> rules;
    private final MeterRegistry meterRegistry;

    public RateLimitManager(
            RateLimitStore store,
            RateLimitProperties properties,
            MeterRegistry meterRegistry) {
        this.store = store;
        this.rules = properties.getRules();
        this.meterRegistry = meterRegistry;
    }

    public RateLimitResult evaluate(HttpServletRequest request) {
        String user = Optional.ofNullable(request.getHeader("X-User-Id"))
                .orElse("anonymous");
        String endpoint = request.getRequestURI();

        boolean allowed = true;
        long minRemaining = Long.MAX_VALUE;
        long maxRetry = 0;

        for (RateLimitRule rule : rules) {
            String scopeKey = RateLimitKeyBuilder.scopeKey(rule, user, endpoint);
            RateLimitResult result = store.allow(scopeKey, rule);

            log.info("RateLimit Rule: {}-{}, Key: {}, Result: {}",
                    rule.getName(), rule.getAlgorithm(), scopeKey, result);

            if (!result.allowed()) {
                allowed = false;
                maxRetry = Math.max(maxRetry, result.retryAfterMillis());
                meterRegistry.counter("rate_limit.rejected", "rule", rule.getName())
                        .increment();
            }

            minRemaining = Math.min(minRemaining, result.remainingTokens());
        }

        if (minRemaining == Long.MAX_VALUE) {
            minRemaining = 0;
        }

        return new RateLimitResult(allowed, minRemaining, maxRetry);
    }
}