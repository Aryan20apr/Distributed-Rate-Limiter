package com.ratelimiter.core.store;

import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;

public interface RateLimitStore {

    /**
     * Atomically evaluate whether one request is allowed for the given scope key and rule.
     *
     * @param scopeKey identity segment, e.g. {@code user:alice} or {@code global}
     * @param rule     the rule definition (algorithm, limits, window)
     */
    RateLimitResult allow(String scopeKey, RateLimitRule rule);
}