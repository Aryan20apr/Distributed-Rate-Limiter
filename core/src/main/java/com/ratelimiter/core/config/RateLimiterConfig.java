package com.ratelimiter.core.config;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.service.RateLimitManager;
import com.ratelimiter.core.store.InMemoryRateLimitStore;
import com.ratelimiter.core.store.RateLimitStore;
import com.ratelimiter.core.store.RateLimiterFactory;
import com.ratelimiter.core.store.RedisRateLimitStore;
import com.ratelimiter.core.web.RateLimitFilter;
import com.ratelimiter.core.web.RateLimitResponseWriter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Configuration
public class RateLimiterConfig {

    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(
            RateLimitManager manager,
            RateLimitResponseWriter responseWriter) {
        FilterRegistrationBean<RateLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RateLimitFilter(manager, responseWriter));
        bean.addUrlPatterns("/*");
        bean.setOrder(1);
        return bean;
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "memory", matchIfMissing = true)
    RateLimitStore inMemoryRateLimitStore(RateLimitProperties properties) {
        validateRules(properties);
        return new InMemoryRateLimitStore(properties.getRules());
    }

    @Bean
    @ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "redis")
    RateLimitStore redisRateLimitStore(
            StringRedisTemplate redisTemplate,
            RedisScript<List<Long>> tokenBucketScript,
            RedisScript<List<Long>> slidingWindowCounterScript,
            RateLimitProperties properties) {
        validateRedisRules(properties);
        return new RedisRateLimitStore(
                redisTemplate,
                tokenBucketScript,
                slidingWindowCounterScript,
                properties);
    }

    private static void validateRules(RateLimitProperties properties) {
        if (properties.getRules() == null || properties.getRules().isEmpty()) {
            throw new IllegalStateException("rate-limit.rules must not be empty");
        }
    }

    private static void validateRedisRules(RateLimitProperties properties) {
        validateRules(properties);
        for (RateLimitRule rule : properties.getRules()) {
            if (!RateLimiterFactory.isRedisSupported(rule.getAlgorithm())) {
                throw new IllegalStateException(
                        "Rule '" + rule.getName() + "' uses algorithm '"
                        + rule.getAlgorithm()
                        + "' which is not supported with rate-limit.store=redis. "
                        + "Use 'token' or 'sliding-counter'.");
            }
        }

    }
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
