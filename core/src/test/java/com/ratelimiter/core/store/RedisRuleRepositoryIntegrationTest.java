package com.ratelimiter.core.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RedisRuleRepository;
import com.redis.testcontainers.RedisContainer;

@Testcontainers
@SpringBootTest(classes = RedisStoreTestConfiguration.class)
class RedisRuleRepositoryIntegrationTest {

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

    private RedisRuleRepository repository;

    @BeforeEach
    void setUp() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        repository = new RedisRuleRepository(redisTemplate, new ObjectMapper());
    }

    @Test
    void saveAndFindByNameRoundTrip() {
        RateLimitRule rule = sampleRule("round-trip");

        repository.save(rule);

        assertThat(repository.findByName("round-trip"))
                .isPresent()
                .get()
                .extracting(RateLimitRule::getAlgorithm, RateLimitRule::getCapacity)
                .containsExactly("token", 10.0);
    }

    @Test
    void findAllReturnsPriorityOrder() {
        RateLimitRule zebra = sampleRule("zebra-rule", 20);
        RateLimitRule alpha = sampleRule("alpha-rule", 10);

        repository.save(zebra);
        repository.save(alpha);

        List<RateLimitRule> rules = repository.findAll();

        assertThat(rules).extracting(RateLimitRule::getName)
                .containsExactly("alpha-rule", "zebra-rule");
    }

    @Test
    void deleteRemovesRule() {
        repository.save(sampleRule("to-delete"));

        repository.delete("to-delete");

        assertThat(repository.findByName("to-delete")).isEmpty();
    }

    @Test
    void isEmptyFalseAfterSave() {
        assertThat(repository.isEmpty()).isTrue();

        repository.save(sampleRule("persisted"));

        assertThat(repository.isEmpty()).isFalse();
    }

    private static RateLimitRule sampleRule(String name) {
        return sampleRule(name, 10);
    }

    private static RateLimitRule sampleRule(String name, int priority) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        rule.setPriority(priority);
        return rule;
    }
}
