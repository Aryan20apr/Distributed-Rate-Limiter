package com.ratelimiter.core.web;
import java.io.IOException;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ratelimiter.core.utils.RateLimitExceededException;

import jakarta.servlet.http.HttpServletResponse;

@ControllerAdvice
public class RateLimitExceptionHandler {

    private final RateLimitResponseWriter responseWriter;

    public RateLimitExceptionHandler(RateLimitResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public void handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletResponse response) throws IOException {
        responseWriter.writeTooManyRequests(response, ex.decision());
    }
}