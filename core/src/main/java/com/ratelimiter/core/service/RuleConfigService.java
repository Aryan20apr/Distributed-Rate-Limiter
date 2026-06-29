package com.ratelimiter.core.service;

import java.time.Instant;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RuleRepository;
import com.ratelimiter.core.utils.RuleConflictException;
import com.ratelimiter.core.utils.RuleNotFoundException;
import com.ratelimiter.core.utils.RuleValidator;

@Service
@ConditionalOnBean(RuleRepository.class)
public class RuleConfigService {

    private final RuleRepository repository;
    private final RuleCatalog catalog;
    private final RateLimitProperties properties;

    public RuleConfigService(
            RuleRepository repository,
            RuleCatalog catalog,
            RateLimitProperties properties) {
        this.repository = repository;
        this.catalog = catalog;
        this.properties = properties;
    }

    public List<RateLimitRule> listRules() {
        return repository.findAll();
    }

    public RateLimitRule getRule(String name) {
        return repository.findByName(name)
                .orElseThrow(() -> new RuleNotFoundException(name));
    }

    public RateLimitRule createRule(RateLimitRule rule) {
        if (repository.findByName(rule.getName()).isPresent()) {
            throw new RuleConflictException("Rule already exists: " + rule.getName());
        }
        validate(rule);
        stamp(rule);
        repository.save(rule);
        repository.incrementConfigVersion();
        reloadLocalCatalog();
        repository.publishChange(rule.getName());
        return rule;
    }

    public RateLimitRule updateRule(String name, RateLimitRule rule) {
        if (!repository.findByName(name).isPresent()) {
            throw new RuleNotFoundException(name);
        }
        rule.setName(name);
        validate(rule);
        stamp(rule);
        repository.save(rule);
        repository.incrementConfigVersion();
        reloadLocalCatalog();
        repository.publishChange(name);
        return rule;
    }

    public void deleteRule(String name) {
        if (!repository.findByName(name).isPresent()) {
            throw new RuleNotFoundException(name);
        }
        repository.delete(name);
        repository.incrementConfigVersion();
        reloadLocalCatalog();
        repository.publishChange(name);
    }

    public void reloadLocalCatalog() {
        catalog.replaceAll(repository.findAll());
    }

    public void reloadRule(String name) {
        if ("*".equals(name)) {
            reloadLocalCatalog();
            return;
        }
        repository.findByName(name).ifPresentOrElse(
                ignored -> reloadLocalCatalog(),
                () -> reloadLocalCatalog()); // full reload keeps ordering consistent
    }

    private void validate(RateLimitRule rule) {
        RuleValidator.validateForRedis(rule);
    }

    private void stamp(RateLimitRule rule) {
        rule.setUpdatedAtEpochMillis(Instant.now().getEpochSecond());
    }
}