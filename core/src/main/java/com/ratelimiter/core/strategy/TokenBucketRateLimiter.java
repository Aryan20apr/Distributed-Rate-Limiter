package com.ratelimiter.core.strategy;

import com.ratelimiter.core.dtos.RateLimitResult;

public class TokenBucketRateLimiter extends AbstractRateLimiter {

    private static class BucketState implements ExpirableState {
        double tokens;
        long lastRefill;
        volatile long lastAccessNanos;
        @Override
        public long getLastAccessNanos() {
            return lastAccessNanos;
        }
    }

    private final double capacity;
    private final double refillPerNano;

    public TokenBucketRateLimiter(double capacity,
                                    double refillPerSecond,
                                    int stripes,
                                    int maxKeys,
                                    long ttlMillis) {
        super(stripes, maxKeys, ttlMillis);
        this.capacity = capacity;
        this.refillPerNano = refillPerSecond / 1_000_000_000.0;
    }

    @Override
    public RateLimitResult allow(String key) {

        long limit = (long) capacity;
        long now = System.nanoTime();

        if (isKeyLimitExceeded(key)) {
            long resetAt = (System.currentTimeMillis() / 1000) + 60;
            return new RateLimitResult(false, 0, 0, limit, resetAt);
        }

        BucketState state = (BucketState) store.computeIfAbsent(key, k -> {
            BucketState bs = new BucketState();
            bs.tokens = capacity;
            bs.lastRefill = now;
            bs.lastAccessNanos = now;
            return bs;
        });

        Object lock = stripe(key);

        synchronized (lock) {

            state.lastAccessNanos = now;

            long delta = now - state.lastRefill;
            double refill = delta * refillPerNano;

            state.tokens = Math.min(capacity, state.tokens + refill);
            state.lastRefill = now;

            if (state.tokens >= 1) {
                state.tokens -= 1;
                long resetAt = (System.currentTimeMillis() / 1000) + 60;
                return new RateLimitResult(true, (long) state.tokens, 0, limit, resetAt);
            }

            long retry = (long) ((1 - state.tokens) / refillPerNano) / 1_000_000;
            long resetAt = (System.currentTimeMillis() + retry) / 1000;
            return new RateLimitResult(false, 0, retry, limit, resetAt);
        }
    }
}
