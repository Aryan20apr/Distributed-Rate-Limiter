package com.ratelimiter.core.repositories;

import java.util.List;
import java.util.Optional;

import com.ratelimiter.core.dtos.RateLimitRule;

public interface RuleRepository {

    String RULES_KEY = "rl:config:rules";
    String META_KEY = "rl:config:meta";
    String CHANGED_CHANNEL = "rl:config:changed";

    boolean isEmpty();

    List<RateLimitRule> findAll();

    Optional<RateLimitRule> findByName(String name);

    void save(RateLimitRule rule);

    void delete(String name);

    void saveAll(List<RateLimitRule> rules);

    void publishChange(String ruleName);

    long incrementConfigVersion();
}





