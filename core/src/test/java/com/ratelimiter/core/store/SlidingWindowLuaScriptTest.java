package com.ratelimiter.core.store;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import com.redis.testcontainers.RedisContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = RedisStoreTestConfiguration.class)
class SlidingWindowCounterLuaScriptTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("rate-limit.redis.host", redis::getHost);
        registry.add("rate-limit.redis.port", () -> redis.getMappedPort(6379));
        registry.add("rate-limit.store", () -> "redis");
    }

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedisScript<List<Long>> slidingWindowCounterScript;

    private RedisRateLimitStore store;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        RateLimitProperties properties = new RateLimitProperties();
        properties.setFailureMode(RateLimitProperties.FailureMode.CLOSED);

        store = new RedisRateLimitStore(
                redisTemplate,
                null,
                slidingWindowCounterScript,
                properties);
    }

    @Test
    void allowsMaxRequestsThenDenies() {
        RateLimitRule rule = slidingCounterRule("user-window", 10, 60_000);

        for (int i = 0; i < 10; i++) {
            RateLimitResult result = store.allow("user:demo-user", rule);
            assertThat(result.allowed())
                    .as("request %d should be allowed", i + 1)
                    .isTrue();
        }

        RateLimitResult denied = store.allow("user:demo-user", rule);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMillis()).isGreaterThan(0);
    }

    private static RateLimitRule slidingCounterRule(String name, long maxRequests, long windowMillis) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("sliding-counter");
        rule.setMaxRequests(maxRequests);
        rule.setWindowMillis(windowMillis);
        return rule;
    }
}