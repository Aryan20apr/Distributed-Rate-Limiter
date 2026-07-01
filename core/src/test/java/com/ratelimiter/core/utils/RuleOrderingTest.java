package com.ratelimiter.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;

class RuleOrderingTest {

    @Test
    void sortedOrdersByPriorityThenName() {
        RateLimitRule zebra = rule("zebra", 20);
        RateLimitRule alpha = rule("alpha", 10);
        RateLimitRule beta = rule("beta", 10);

        List<RateLimitRule> sorted = RuleOrdering.sorted(List.of(zebra, alpha, beta));

        assertThat(sorted).extracting(RateLimitRule::getName)
                .containsExactly("alpha", "beta", "zebra");
    }

    @Test
    void stampFromListOrderIfUnsetAssignsDecadeSteps() {
        List<RateLimitRule> rules = new ArrayList<>();
        rules.add(rule("first", 0));
        rules.add(rule("second", 0));
        rules.add(ruleWithPriority("third", 50));

        RuleOrdering.stampFromListOrderIfUnset(rules);

        assertThat(rules).extracting(RateLimitRule::getPriority)
                .containsExactly(10, 20, 50);
    }

    @Test
    void nextPriorityReturnsStepAboveMax() {
        assertThat(RuleOrdering.nextPriority(List.of())).isEqualTo(10);
        assertThat(RuleOrdering.nextPriority(List.of(
                ruleWithPriority("a", 10),
                ruleWithPriority("b", 30))))
                .isEqualTo(40);
    }

    private static RateLimitRule rule(String name, int priority) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        rule.setPriority(priority);
        return rule;
    }

    private static RateLimitRule ruleWithPriority(String name, int priority) {
        return rule(name, priority);
    }
}
