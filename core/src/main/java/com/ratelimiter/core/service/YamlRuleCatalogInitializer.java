package com.ratelimiter.core.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.utils.RuleOrdering;

@Component
@ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "memory", matchIfMissing = true)
public class YamlRuleCatalogInitializer {
    public YamlRuleCatalogInitializer(RuleCatalog catalog, RateLimitProperties properties) {
        List<RateLimitRule> rules = new ArrayList<>(properties.getRules());
        RuleOrdering.stampFromListOrderIfUnset(rules);
        catalog.replaceAll(rules);
    }
}
