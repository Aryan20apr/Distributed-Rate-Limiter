package com.ratelimiter.core.service;


import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import static com.ratelimiter.core.config.RateLimitScope.API_KEY;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.identity.ClientIdentity;

@Component
public class RuleMatcher {

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public boolean applies(RateLimitRule rule, ClientIdentity identity, String endpoint) {
        if (!rule.isEnabled()) {
            return false;
        }
        if (rule.getScope() == API_KEY
                && identity.apiKey() == null) {
            return false;
        }
        String pattern = rule.getEndpointPattern();
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        return pathMatcher.match(pattern, endpoint);
    }
}





