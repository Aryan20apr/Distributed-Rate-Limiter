package com.ratelimiter.core.store;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.redis.testcontainers.RedisContainer;

@Testcontainers
@SpringBootTest(classes = RedisStoreTestConfiguration.class)
class TokenBucketLuaScriptTest {

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("rate-limit.redis.host", redis::getHost);
        registry.add("rate-limit.redis.port", () -> redis.getMappedPort(6379));
        registry.add("rate-limit.store", () -> "redis");
        registry.add("rate-limit.failure-mode", () -> "closed");
    }

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedisScript<List<Long>> tokenBucketScript;

    private RedisRateLimitStore store;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();

        RateLimitProperties properties = new RateLimitProperties();
        properties.setFailureMode(RateLimitProperties.FailureMode.CLOSED);

        store = new RedisRateLimitStore(
                redisTemplate,
                tokenBucketScript,
                null,
                properties);
    }

    @Test
    void allowsUpToCapacityThenDenies() {
        RateLimitRule rule = tokenRule("burst-test", 5, 1.0);

        for (int i = 0; i < 5; i++) {
            RateLimitResult result = store.allow("user:alice", rule);
            assertThat(result.allowed()).isTrue();
        }

        RateLimitResult denied = store.allow("user:alice", rule);
        assertThat(denied.allowed()).isFalse();
        assertThat(denied.retryAfterMillis()).isGreaterThan(0);
    }

    @Test
    void separateKeysAreIndependent() {
        RateLimitRule rule = tokenRule("isolation-test", 2, 1.0);

        assertThat(store.allow("user:alice", rule).allowed()).isTrue();
        assertThat(store.allow("user:alice", rule).allowed()).isTrue();
        assertThat(store.allow("user:alice", rule).allowed()).isFalse();

        assertThat(store.allow("user:bob", rule).allowed()).isTrue();
    }

    private static RateLimitRule tokenRule(String name, double capacity, double refillPerSecond) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(capacity);
        rule.setRefillPerSecond(refillPerSecond);
        rule.setWindowMillis(60_000);
        return rule;
    }
}