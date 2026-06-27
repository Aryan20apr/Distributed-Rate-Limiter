package com.ratelimiter.core.strategy;

import java.util.concurrent.atomic.LongAdder;

import com.ratelimiter.core.dtos.RateLimitResult;

public class FixedWindowRateLimiter extends AbstractRateLimiter {

    private static class WindowState implements ExpirableState {
        volatile long windowStart;
        final LongAdder counter = new LongAdder();
        volatile long lastAccessNanos;
        @Override
        public long getLastAccessNanos() {
            return lastAccessNanos;
        }
    }

    private final long windowSizeNanos;
    private final long maxRequests;

    public FixedWindowRateLimiter(long maxRequests,
                                    long windowSizeMillis,
                                    int stripes,
                                    int maxKeys,
                                    long ttlMillis) {
        super(stripes, maxKeys, ttlMillis);
        this.maxRequests = maxRequests;
        this.windowSizeNanos = windowSizeMillis * 1_000_000;
    }

    @Override
    public RateLimitResult allow(String key) {

        long limit = maxRequests;
        long now = System.nanoTime();

        if (isKeyLimitExceeded(key)) {
            return new RateLimitResult(false, 0, 0, limit, (System.currentTimeMillis() / 1000) + 60);
        }

        WindowState state = (WindowState) store.computeIfAbsent(key, k -> {
            WindowState ws = new WindowState();
            ws.windowStart = now;
            ws.lastAccessNanos = now;
            return ws;
        });

        Object lock = stripe(key);

        synchronized (lock) {

            state.lastAccessNanos = now;

            long elapsed = now - state.windowStart;
            if (elapsed >= windowSizeNanos) {
                state.windowStart = now;
                state.counter.reset();
                elapsed = 0;
            }

            state.counter.increment();
            long count = state.counter.sum();

            long retryMillis = (windowSizeNanos - elapsed) / 1_000_000;
            long resetAt = (System.currentTimeMillis() + retryMillis) / 1000;

            if (count <= maxRequests) {
                return new RateLimitResult(true, maxRequests - count, 0, limit, resetAt);
            }

            return new RateLimitResult(false, 0, retryMillis, limit, resetAt);
        }
    }
}
