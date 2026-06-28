package com.ratelimiter.core.config;

public enum RateLimitScope {
    GLOBAL,
    USER,
    IP,
    API_KEY,
    ENDPOINT,
    USER_ENDPOINT
}
