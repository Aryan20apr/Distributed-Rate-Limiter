package com.ratelimiter.core.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitDecision;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.identity.ClientIdentity;
import com.ratelimiter.core.identity.ClientIdentityResolver;
import com.ratelimiter.core.store.RateLimitKeyBuilder;
import com.ratelimiter.core.store.RateLimitStore;
import com.ratelimiter.core.utils.RuleLimitResolver;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RateLimitManager {

    private final RateLimitStore store;
    // private final List<RateLimitRule> rules;
    private final MeterRegistry meterRegistry;
    private final ClientIdentityResolver identityResolver;
    private final RuleMatcher ruleMatcher;
    private final RuleCatalog ruleCatalog;

    public RateLimitManager(
            RateLimitStore store,
            RateLimitProperties properties,
            MeterRegistry meterRegistry,
            ClientIdentityResolver identityResolver,
            RuleCatalog ruleCatalog,
            RuleMatcher ruleMatcher) {
        this.store = store;
        this.ruleCatalog = ruleCatalog;
        this.meterRegistry = meterRegistry;
        this.identityResolver = identityResolver;
        this.ruleMatcher = ruleMatcher;
    }

    public RateLimitDecision evaluate(HttpServletRequest request) {
        ClientIdentity identity = identityResolver.resolve(request);
        String endpoint = request.getRequestURI();

        long tightestRemaining = Long.MAX_VALUE;
        long headerLimit = 0;
        long headerResetAt = 0;
        RateLimitRule tightestRule = null;

        for (RateLimitRule rule : ruleCatalog.getRules()) {
            if (!ruleMatcher.applies(rule, identity, endpoint)) {
                continue;
            }

            String scopeKey = RateLimitKeyBuilder.scopeKey(rule, identity, endpoint);
            RateLimitResult result = store.allow(scopeKey, rule);

            long ruleLimit = result.limit() > 0
                    ? result.limit()
                    : RuleLimitResolver.resolveLimit(rule);
            long resetAt = result.resetAtEpochSeconds() > 0
                    ? result.resetAtEpochSeconds()
                    : fallbackResetAt(result.retryAfterMillis());

            log.info("RateLimit Rule: {}-{}, Identity: {}, Key: {}, Result: {}",
                    rule.getName(), rule.getAlgorithm(), identity, scopeKey, result);

            if (!result.allowed()) {
                meterRegistry.counter("rate_limit.rejected", "rule", rule.getName()).increment();
                return RateLimitDecision.denied(
                        rule.getName(),
                        ruleLimit,
                        result.remaining(),
                        resetAt,
                        result.retryAfterMillis());
            }

            if (result.remaining() < tightestRemaining) {
                tightestRemaining = result.remaining();
                headerLimit = ruleLimit;
                headerResetAt = resetAt;
                tightestRule = rule;
            }
        }

        if (tightestRemaining == Long.MAX_VALUE) {
            return RateLimitDecision.allowed(0, 0, 0);
        }

        return RateLimitDecision.allowed(headerLimit, tightestRemaining, headerResetAt);
    }

    private static long fallbackResetAt(long retryAfterMillis) {
        return retryAfterMillis > 0
                ? (System.currentTimeMillis() + retryAfterMillis) / 1000
                : (System.currentTimeMillis() / 1000) + 60;
    }
}