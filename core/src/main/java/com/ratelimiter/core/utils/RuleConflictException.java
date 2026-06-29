package com.ratelimiter.core.utils;

public class RuleConflictException extends RuntimeException {

    public RuleConflictException(String message) {
        super(message);
    }
}
