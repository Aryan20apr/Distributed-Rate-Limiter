package com.ratelimiter.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.identity.ClientIdentity;
import com.ratelimiter.core.identity.ClientIdentityResolver;
import com.ratelimiter.core.store.RateLimitStore;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class RateLimitManagerTest {

    @Mock
    private RateLimitStore store;

    @Mock
    private ClientIdentityResolver identityResolver;

    @Mock
    private HttpServletRequest request;

    private RuleCatalog catalog;
    private RateLimitManager manager;

    @BeforeEach
    void setUp() {
        catalog = new RuleCatalog();
        manager = new RateLimitManager(
                store,
                new RateLimitProperties(),
                new SimpleMeterRegistry(),
                identityResolver,
                catalog,
                new RuleMatcher());
        when(request.getRequestURI()).thenReturn("/test");
        when(identityResolver.resolve(request))
                .thenReturn(new ClientIdentity("user-1", "127.0.0.1", null));
    }

    @Test
    void deniedReturnsFirstRuleInPriorityOrder() {
        RateLimitRule lowerPriority = rule("zebra-rule", 20);
        RateLimitRule higherPriority = rule("alpha-rule", 10);
        catalog.replaceAll(List.of(lowerPriority, higherPriority));

        when(store.allow(anyString(), any(RateLimitRule.class)))
                .thenAnswer(invocation -> {
                    RateLimitRule rule = invocation.getArgument(1);
                    if ("alpha-rule".equals(rule.getName())) {
                        return deniedResult();
                    }
                    return allowedResult();
                });

        var decision = manager.evaluate(request);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.violatedRuleName()).isEqualTo("alpha-rule");
    }

    @Test
    void evaluatesHigherPriorityRuleBeforeLowerPriorityRule() {
        RateLimitRule lowerPriority = rule("zebra-rule", 20);
        RateLimitRule higherPriority = rule("alpha-rule", 10);
        catalog.replaceAll(List.of(lowerPriority, higherPriority));

        when(store.allow(anyString(), any(RateLimitRule.class))).thenReturn(allowedResult());

        manager.evaluate(request);

        var inOrder = org.mockito.Mockito.inOrder(store);
        inOrder.verify(store).allow(anyString(), org.mockito.ArgumentMatchers.eq(higherPriority));
        inOrder.verify(store).allow(anyString(), org.mockito.ArgumentMatchers.eq(lowerPriority));
    }

    private static RateLimitRule rule(String name, int priority) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        rule.setPriority(priority);
        rule.setEnabled(true);
        return rule;
    }

    private static RateLimitResult deniedResult() {
        return new RateLimitResult(false, 0, 1000, 10, 0);
    }

    private static RateLimitResult allowedResult() {
        return new RateLimitResult(true, 5, 0, 10, 0);
    }
}
