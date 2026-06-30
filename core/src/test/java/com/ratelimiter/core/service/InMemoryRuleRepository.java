package com.ratelimiter.core.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RuleRepository;

class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, RateLimitRule> rules = new ConcurrentHashMap<>();
    private final List<String> published = new CopyOnWriteArrayList<>();
    private long version;

    @Override
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    @Override
    public List<RateLimitRule> findAll() {
        return rules.values().stream()
                .sorted(Comparator.comparing(RateLimitRule::getName))
                .toList();
    }

    @Override
    public Optional<RateLimitRule> findByName(String name) {
        return Optional.ofNullable(rules.get(name));
    }

    @Override
    public void save(RateLimitRule rule) {
        rules.put(rule.getName(), rule);
    }

    @Override
    public void delete(String name) {
        rules.remove(name);
    }

    @Override
    public void saveAll(List<RateLimitRule> rules) {
        rules.forEach(this::save);
    }

    @Override
    public void publishChange(String ruleName) {
        published.add(ruleName);
    }

    @Override
    public long incrementConfigVersion() {
        return ++version;
    }

    List<String> publishedChanges() {
        return List.copyOf(published);
    }
}
