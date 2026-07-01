package com.ratelimiter.core.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.AdminErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AdminAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    public AdminAuthFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.getAdmin().isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return !path.startsWith("/admin/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String expected = properties.getAdmin().getToken();
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header != null
                && header.startsWith(BEARER_PREFIX)
                && expected.equals(header.substring(BEARER_PREFIX.length()).trim())) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getOutputStream().write(
                objectMapper.writeValueAsString(
                        AdminErrorResponse.of("unauthorized", "Invalid or missing admin token"))
                        .getBytes(StandardCharsets.UTF_8));
    }
}
