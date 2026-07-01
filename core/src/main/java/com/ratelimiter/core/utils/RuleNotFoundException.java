package com.ratelimiter.core.utils;

public class RuleNotFoundException extends RuntimeException {

    public RuleNotFoundException(String name) {
        super("Rule not found: " + name);
    }
}
