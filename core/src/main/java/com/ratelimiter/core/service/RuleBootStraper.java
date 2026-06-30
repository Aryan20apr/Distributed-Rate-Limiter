package com.ratelimiter.core.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.repositories.RuleRepository;
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
            List<RateLimitRule> yamlRules = properties.getRules();
            if (yamlRules == null || yamlRules.isEmpty()) {
                throw new IllegalStateException(
                        "Redis rule config is empty and rate-limit.rules YAML is empty");
            }
            log.info("Bootstrapping {} rules from YAML into Redis", yamlRules.size());
            for (RateLimitRule rule : yamlRules) {
                RuleValidator.validateForRedis(rule);
            }
            repository.saveAll(yamlRules);
            repository.incrementConfigVersion();
            repository.publishChange("*");
        }
        configService.reloadLocalCatalog();
        log.info("Rule catalog loaded: {} rules", configService.listRules().size());
    }
}