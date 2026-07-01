package com.ratelimiter.core.dtos;

import com.ratelimiter.core.config.RateLimitScope;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRuleRequest {

    @NotBlank
    private String name;

    @NotNull
    private RateLimitScope scope;

    @NotBlank
    private String algorithm;

    private long maxRequests;
    private long windowMillis;
    private double refillPerSecond;
    private double capacity;
    private String endpointPattern;
    private boolean enabled = true;
    private int priority;

    public RateLimitRule toRule() {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(scope);
        rule.setAlgorithm(algorithm);
        rule.setMaxRequests(maxRequests);
        rule.setWindowMillis(windowMillis);
        rule.setRefillPerSecond(refillPerSecond);
        rule.setCapacity(capacity);
        rule.setEndpointPattern(endpointPattern);
        rule.setEnabled(enabled);
        rule.setPriority(priority);
        return rule;
    }
}