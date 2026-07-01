package com.ratelimiter.core.utils;

import java.util.Map;

public class RuleValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public RuleValidationException(Map<String, String> fieldErrors) {
        super("Rule validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}