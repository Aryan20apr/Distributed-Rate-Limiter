package com.ratelimiter.core.web;

import java.io.IOException;

import com.ratelimiter.core.dtos.RateLimitDecision;
import com.ratelimiter.core.service.RateLimitManager;
import com.ratelimiter.core.store.RateLimitStoreException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitFilter implements Filter {

    private final RateLimitManager manager;

    public RateLimitFilter(RateLimitManager manager) {
        this.manager = manager;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var httpRes = (HttpServletResponse) response;

        try {
            RateLimitDecision result = manager.evaluate(httpReq);

            if (!result.allowed()) {
                httpRes.setStatus(429);
                httpRes.setHeader("Retry-After",
                        String.valueOf(Math.max(1, result.retryAfterMillis() / 1000)));
                httpRes.getWriter().write("Rate limit exceeded");
                return;
            }

            httpRes.setHeader("X-RateLimit-Remaining",
                    String.valueOf(result.remaining()));

            chain.doFilter(request, response);
        } catch (RateLimitStoreException ex) {
            httpRes.setStatus(503);
            httpRes.getWriter().write("Rate limiter unavailable");
        }
    }
}