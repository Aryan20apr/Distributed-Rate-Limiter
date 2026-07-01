package com.ratelimiter.core.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.ratelimiter.core.dtos.RateLimitRule;

@Component
public class RuleCatalog {

    private final AtomicReference<List<RateLimitRule>> rules =
            new AtomicReference<>(List.of());

    public List<RateLimitRule> getRules() {
        return rules.get();
    }

    public void replaceAll(List<RateLimitRule> newRules) {
        rules.set(List.copyOf(newRules));
    }

    public void clear() {
        rules.set(List.of());
    }

    public boolean isEmpty() {
        return rules.get().isEmpty();
    }
}





