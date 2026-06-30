package com.ratelimiter.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RateLimitScope;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.utils.RuleConflictException;
import com.ratelimiter.core.utils.RuleNotFoundException;

class RuleConfigServiceTest {

    private InMemoryRuleRepository repository;
    private RuleCatalog catalog;
    private RuleConfigService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRuleRepository();
        catalog = new RuleCatalog();
        service = new RuleConfigService(repository, catalog, new RateLimitProperties());
    }

    @Test
    void createRuleSavesReloadsCatalogAndPublishes() {
        RateLimitRule created = service.createRule(validTokenRule("my-rule"));

        assertThat(created.getName()).isEqualTo("my-rule");
        assertThat(repository.findByName("my-rule")).isPresent();
        assertThat(catalog.getRules()).extracting(RateLimitRule::getName).contains("my-rule");
        assertThat(repository.publishedChanges()).contains("my-rule");
    }

    @Test
    void duplicateCreateThrowsRuleConflictException() {
        service.createRule(validTokenRule("dup-rule"));

        assertThatThrownBy(() -> service.createRule(validTokenRule("dup-rule")))
                .isInstanceOf(RuleConflictException.class);
    }

    @Test
    void updateRuleNotFoundThrowsRuleNotFoundException() {
        assertThatThrownBy(() -> service.updateRule("missing", validTokenRule("missing")))
                .isInstanceOf(RuleNotFoundException.class);
    }

    @Test
    void deleteRuleRemovesFromCatalog() {
        service.createRule(validTokenRule("to-delete"));

        service.deleteRule("to-delete");

        assertThat(repository.findByName("to-delete")).isEmpty();
        assertThat(catalog.getRules()).isEmpty();
        assertThat(repository.publishedChanges()).contains("to-delete");
    }

    private static RateLimitRule validTokenRule(String name) {
        RateLimitRule rule = new RateLimitRule();
        rule.setName(name);
        rule.setScope(RateLimitScope.USER);
        rule.setAlgorithm("token");
        rule.setCapacity(10);
        rule.setRefillPerSecond(1.0);
        return rule;
    }
}
