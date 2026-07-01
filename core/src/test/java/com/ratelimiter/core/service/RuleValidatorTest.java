package com.ratelimiter.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.utils.RuleValidationException;
import com.ratelimiter.core.utils.RuleValidator;

class RuleValidatorTest {

    @Test
    void validTokenRulePasses() {
        RateLimitRule rule = tokenRule("token-rule");

        assertThatCode(() -> RuleValidator.validateForRedis(rule)).doesNotThrowAnyException();
    }

    @Test
    void missingNameProducesFieldError() {
        RateLimitRule rule = tokenRule(null);
        rule.setName(null);

        assertThatThrownBy(() -> RuleValidator.validateForRedis(rule))
                .isInstanceOf(RuleValidationException.class)
                .satisfies(ex -> assertThat(((RuleValidationException) ex).fieldErrors())
                        .containsEntry("name", "name is required"));
    }

    @Test
    void fixedAlgorithmWithRedisValidationProducesAlgorithmFieldError() {
        RateLimitRule rule = new RateLimitRule();
        rule.setName("fixed-rule");
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("fixed");
        rule.setMaxRequests(10);
        rule.setWindowMillis(60_000);

        assertThatThrownBy(() -> RuleValidator.validateForRedis(rule))
                .isInstanceOf(RuleValidationException.class)
                .satisfies(ex -> assertThat(((RuleValidationException) ex).fieldErrors())
                        .containsKey("algorithm"));
    }

    @Test
    void slidingCounterWithZeroMaxRequestsProducesFieldError() {
        RateLimitRule rule = new RateLimitRule();
        rule.setName("sliding-rule");
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("sliding-counter");
        rule.setMaxRequests(0);
        rule.setWindowMillis(60_000);

        assertThatThrownBy(() -> RuleValidator.validateForRedis(rule))
                .isInstanceOf(RuleValidationException.class)
                .satisfies(ex -> assertThat(((RuleValidationException) ex).fieldErrors())
                        .containsEntry("maxRequests", "maxRequests must be > 0"));
    }

    private static RateLimitRule tokenRule(String name) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        return rule;
    }
}
