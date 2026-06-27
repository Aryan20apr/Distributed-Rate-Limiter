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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = RedisStoreTestConfiguration.class)
class RedisRateLimitStoreIntegrationTest {

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
    RedisScript<List<Long>> tokenBucketScript;

    private RedisRateLimitStore store;
    private RateLimitRule rule;

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

        rule = new RateLimitRule();
        rule.setName("concurrent-test");
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(0);
        rule.setWindowMillis(60_000);
    }

    @Test
    void concurrentRequestsDoNotOverAllow() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger allowed = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    RateLimitResult result = store.allow("user:alice", rule);
                    if (result.allowed()) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await();
        executor.shutdown();

        assertThat(allowed.get()).isEqualTo(10);
    }
}
