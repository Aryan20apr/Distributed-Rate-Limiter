package com.ratelimiter.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RuleRepository;
import com.ratelimiter.core.utils.RuleOrdering;
import com.ratelimiter.core.utils.RuleValidator;

import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnBean(RuleRepository.class)
@Slf4j
public class RuleBootStraper {

    private final RuleRepository repository;
    private final RuleConfigService configService;
    private final RateLimitProperties properties;

    public RuleBootStraper(
            RuleRepository repository,
            RuleConfigService configService,
            RateLimitProperties properties) {
        this.repository = repository;
        this.configService = configService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        if (!properties.getAdmin().isEnabled()) {
            configService.reloadLocalCatalog();
            return;
        }
        if (repository.isEmpty()) {
            List<RateLimitRule> yamlRules = new ArrayList<>(properties.getRules());
            if (yamlRules.isEmpty()) {
                throw new IllegalStateException(
                        "Redis rule config is empty and rate-limit.rules YAML is empty");
            }
            log.info("Bootstrapping {} rules from YAML into Redis", yamlRules.size());
            RuleOrdering.stampFromListOrderIfUnset(yamlRules);
            for (RateLimitRule rule : yamlRules) {
                RuleValidator.validateForRedis(rule);
            }
            repository.saveAll(yamlRules);
            repository.incrementConfigVersion();
            repository.publishChange("*");
        } else {
            migrateLegacyPrioritiesIfNeeded();
        }
        configService.reloadLocalCatalog();
        log.info("Rule catalog loaded: {} rules", configService.listRules().size());
    }

    private void migrateLegacyPrioritiesIfNeeded() {
        List<RateLimitRule> existing = repository.findAll();
        if (existing.isEmpty()
                || existing.stream().anyMatch(rule -> !RuleOrdering.isUnset(rule.getPriority()))) {
            return;
        }

        Map<String, Integer> yamlIndexByName = new HashMap<>();
        List<RateLimitRule> yamlRules = properties.getRules();
        if (yamlRules != null) {
            for (int i = 0; i < yamlRules.size(); i++) {
                RateLimitRule yamlRule = yamlRules.get(i);
                if (yamlRule.getName() != null) {
                    yamlIndexByName.put(yamlRule.getName(), i);
                }
            }
        }

        List<RateLimitRule> migrated = new ArrayList<>();
        for (RateLimitRule rule : existing) {
            Integer yamlIndex = yamlIndexByName.get(rule.getName());
            if (yamlIndex != null) {
                rule.setPriority((yamlIndex + 1) * 10);
            } else {
                rule.setPriority(RuleOrdering.legacyUnknownPriority());
            }
            migrated.add(rule);
        }

        repository.saveAll(migrated);
        repository.incrementConfigVersion();
        repository.publishChange("*");
        log.info("Migrated {} legacy rules to priority ordering", migrated.size());
    }
}
