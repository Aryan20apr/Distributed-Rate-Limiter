package com.ratelimiter.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.identity.ClientIdentity;

class RuleMatcherTest {

    private final RuleMatcher matcher = new RuleMatcher();

    @Test
    void appliesWhenEndpointPatternMatches() {
        RateLimitRule rule = apiKeyRule("/api/**");
        ClientIdentity identity = new ClientIdentity("alice", "127.0.0.1", "sk-demo");

        assertThat(matcher.applies(rule, identity, "/api/data")).isTrue();
        assertThat(matcher.applies(rule, identity, "/test")).isFalse();
    }

    @Test
    void skipsDisabledRules() {
        RateLimitRule rule = apiKeyRule("/api/**");
        rule.setEnabled(false);
        ClientIdentity identity = new ClientIdentity("alice", "127.0.0.1", "sk-demo");

        assertThat(matcher.applies(rule, identity, "/api/data")).isFalse();
    }

    @Test
    void skipsApiKeyScopeWhenApiKeyMissing() {
        RateLimitRule rule = apiKeyRule(null);
        ClientIdentity identity = new ClientIdentity("alice", "127.0.0.1", null);

        assertThat(matcher.applies(rule, identity, "/api/data")).isFalse();
    }

    @Test
    void appliesWhenNoEndpointPattern() {
        RateLimitRule rule = apiKeyRule(null);
        ClientIdentity identity = new ClientIdentity("alice", "127.0.0.1", "sk-demo");

        assertThat(matcher.applies(rule, identity, "/any/path")).isTrue();
    }

    private static RateLimitRule apiKeyRule(String endpointPattern) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName("api-key-limit");
        rule.setScope(RateLimitScope.API_KEY);
        rule.setAlgorithm("token");
        rule.setCapacity(50);
        rule.setRefillPerSecond(1.0);
        rule.setEndpointPattern(endpointPattern);
        return rule;
    }
}
