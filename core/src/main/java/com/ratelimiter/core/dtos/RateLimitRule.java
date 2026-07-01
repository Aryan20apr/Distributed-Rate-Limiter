package com.ratelimiter.core.dtos;

import com.ratelimiter.core.config.RateLimitScope;

import lombok.Data;

@Data
public class RateLimitRule {

    private String name;
    private RateLimitScope scope;
    private String algorithm;
    private long maxRequests;
    private long windowMillis;
    private double refillPerSecond;
    private double capacity;

    /** Ant-style pattern, e.g. /api/search, /api/** — null means all endpoints */
    private String endpointPattern;

    private boolean enabled = true;

    /**
     * Enforcement order: lower value is evaluated first. 0 means unset and is auto-assigned
     * on bootstrap or admin create.
     */
    private int priority;

    /** Unix epoch seconds when this rule was last written. Not required in create requests. */
    private long updatedAtEpochMillis;






}
