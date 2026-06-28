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
    private final RateLimitResponseWriter responseWriter;

    public RateLimitFilter(RateLimitManager manager, RateLimitResponseWriter responseWriter) {
        this.manager = manager;
        this.responseWriter = responseWriter;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        var httpReq = (HttpServletRequest) request;
        var httpRes = (HttpServletResponse) response;

        try {
            RateLimitDecision decision = manager.evaluate(httpReq);

            if (!decision.allowed()) {
                responseWriter.writeTooManyRequests(httpRes, decision);
                return;
            }

            responseWriter.writeAllowedHeaders(httpRes, decision);
            chain.doFilter(request, response);
        } catch (RateLimitStoreException ex) {
            responseWriter.writeServiceUnavailable(httpRes);
        }
    }
}