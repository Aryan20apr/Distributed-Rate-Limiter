package com.ratelimiter.core.utils;

public class RateLimitStoreException extends RuntimeException {

    private final String message;
    private final Throwable cause;

    public RateLimitStoreException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}