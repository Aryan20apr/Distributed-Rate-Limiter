package com.ratelimiter.core.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.dtos.RateLimitDecision;

class RateLimitResponseWriterTest {

    private RateLimitResponseWriter writer;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        writer = new RateLimitResponseWriter(new ObjectMapper());
        response = new MockHttpServletResponse();
    }

    @Test
    void writeAllowedHeadersSetsThreeHeaders() throws Exception {
        RateLimitDecision decision = RateLimitDecision.allowed(10, 7, 1_719_494_460L);

        writer.writeAllowedHeaders(response, decision);

        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("7");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1719494460");
        assertThat(response.getHeader("Retry-After")).isNull();
    }

    @Test
    void writeTooManyRequestsSetsFourHeadersAndJsonBody() throws Exception {
        RateLimitDecision decision = RateLimitDecision.denied(
                "user-limit", 10, 0, 1_719_494_502L, 42_000L);

        writer.writeTooManyRequests(response, decision);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("1719494502");
        assertThat(response.getHeader("Retry-After")).isEqualTo("42");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"error\":\"rate_limit_exceeded\"")
                .contains("\"message\":\"Rate limit exceeded for rule 'user-limit'.\"")
                .contains("\"retryAfterSeconds\":42");
    }
}
