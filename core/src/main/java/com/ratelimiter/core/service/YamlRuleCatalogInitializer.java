package com.ratelimiter.core.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.ratelimiter.core.config.RateLimitProperties;

@Component
@ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "memory", matchIfMissing = true)
public class YamlRuleCatalogInitializer {
    public YamlRuleCatalogInitializer(RuleCatalog catalog, RateLimitProperties properties) {
        catalog.replaceAll(properties.getRules());
    }
}
