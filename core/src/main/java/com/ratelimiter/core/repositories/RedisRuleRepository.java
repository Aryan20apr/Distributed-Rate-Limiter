package com.ratelimiter.core.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.utils.RateLimitStoreException;
import com.ratelimiter.core.utils.RuleOrdering;

@Repository
@ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "redis")
public class RedisRuleRepository implements RuleRepository {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public RedisRuleRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isEmpty() {
        Long size = redis.opsForHash().size(RULES_KEY);
        return size == null || size == 0L;
    }

    @Override
    public List<RateLimitRule> findAll() {
        Map<Object, Object> entries = redis.opsForHash().entries(RULES_KEY);
        List<RateLimitRule> rules = new ArrayList<>(entries.size());
        for (Object value : entries.values()) {
            rules.add(deserialize((String) value));
        }
        return RuleOrdering.sorted(rules);
    }

    @Override
    public Optional<RateLimitRule> findByName(String name) {
        Object raw = redis.opsForHash().get(RULES_KEY, name);
        return raw == null ? Optional.empty() : Optional.of(deserialize((String) raw));
    }

    @Override
    public void save(RateLimitRule rule) {
        redis.opsForHash().put(RULES_KEY, rule.getName(), serialize(rule));
    }

    @Override
    public void delete(String name) {
        redis.opsForHash().delete(RULES_KEY, name);
    }

    @Override
    public void saveAll(List<RateLimitRule> rules) {
        for (RateLimitRule rule : rules) {
            save(rule);
        }
    }

    @Override
    public void publishChange(String ruleName) {
        redis.convertAndSend(CHANGED_CHANNEL, ruleName);
    }

    @Override
    public long incrementConfigVersion() {
        return redis.opsForHash().increment(META_KEY, "version", 1L);
    }

    private String serialize(RateLimitRule rule) {
        try {
            return objectMapper.writeValueAsString(rule);
        } catch (JsonProcessingException ex) {
            throw new RateLimitStoreException("Failed to serialize rule: " + rule.getName(), ex);
        }
    }

    private RateLimitRule deserialize(String json) {
        try {
            return objectMapper.readValue(json, RateLimitRule.class);
        } catch (JsonProcessingException ex) {
            throw new RateLimitStoreException("Failed to deserialize rule JSON", ex);
        }
    }
}





