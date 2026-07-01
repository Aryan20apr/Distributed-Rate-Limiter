package com.ratelimiter.core.listener;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RuleRepository;
import com.ratelimiter.core.service.RuleCatalog;
import com.redis.testcontainers.RedisContainer;

@Testcontainers
@SpringBootTest(classes = RuleConfigIntegrationTestConfiguration.class)
class RuleConfigChangeListenerIntegrationTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("rate-limit.redis.host", redis::getHost);
        registry.add("rate-limit.redis.port", () -> redis.getMappedPort(6379));
        registry.add("rate-limit.store", () -> "redis");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RuleRepository repository;

    @Autowired
    private RuleCatalog catalog;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(RuleRepository.RULES_KEY);
        redisTemplate.delete(RuleRepository.META_KEY);
        catalog.clear();
    }

    @Test
    void publishTriggersListenerReload() throws Exception {
        repository.save(sampleRule("dynamic-test"));
        repository.publishChange("dynamic-test");

        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (System.nanoTime() < deadline) {
            if (catalog.getRules().stream()
                    .anyMatch(rule -> "dynamic-test".equals(rule.getName()))) {
                break;
            }
            Thread.sleep(50);
        }

        assertThat(catalog.getRules()).extracting(RateLimitRule::getName)
                .contains("dynamic-test");
    }

    private static RateLimitRule sampleRule(String name) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        return rule;
    }
}
