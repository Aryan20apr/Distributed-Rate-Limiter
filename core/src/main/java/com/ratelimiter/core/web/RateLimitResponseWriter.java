package com.ratelimiter.core.web;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.dtos.RateLimitDecision;
import com.ratelimiter.core.dtos.RateLimitErrorResponse;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitResponseWriter {

    private static final String HDR_LIMIT = "X-RateLimit-Limit";
    private static final String HDR_REMAINING = "X-RateLimit-Remaining";
    private static final String HDR_RESET = "X-RateLimit-Reset";
    private static final String HDR_RETRY_AFTER = "Retry-After";

    private final ObjectMapper objectMapper;

    public RateLimitResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeAllowedHeaders(HttpServletResponse response, RateLimitDecision decision) {
        setRateLimitHeaders(response, decision);
    }

    public void writeTooManyRequests(HttpServletResponse response, RateLimitDecision decision)
            throws IOException {
        response.setStatus(429);
        setRateLimitHeaders(response, decision);

        long retrySeconds = Math.max(1, decision.retryAfterMillis() / 1000);
        response.setHeader(HDR_RETRY_AFTER, String.valueOf(retrySeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String ruleName = decision.violatedRuleName() != null
                ? decision.violatedRuleName()
                : "unknown";
        RateLimitErrorResponse body = RateLimitErrorResponse.exceeded(ruleName, retrySeconds);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    public void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setStatus(503);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        new RateLimitErrorResponse(
                                "rate_limiter_unavailable",
                                "Rate limiter is temporarily unavailable.",
                                0)));
    }

    private void setRateLimitHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader(HDR_LIMIT, String.valueOf(decision.limit()));
        response.setHeader(HDR_REMAINING, String.valueOf(Math.max(0, decision.remaining())));
        response.setHeader(HDR_RESET, String.valueOf(decision.resetAtEpochSeconds()));
    }
}