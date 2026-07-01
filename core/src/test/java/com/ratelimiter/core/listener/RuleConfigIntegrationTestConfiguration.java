package com.ratelimiter.core.listener;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RedisConfig;
import com.ratelimiter.core.repositories.RedisRuleRepository;
import com.ratelimiter.core.service.RuleCatalog;
import com.ratelimiter.core.service.RuleConfigChangeListener;
import com.ratelimiter.core.service.RuleConfigService;

@SpringBootConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@Import({
        RuleConfigListenerTestBeans.class,
        RedisConfig.class,
        RedisRuleRepository.class,
        RuleCatalog.class,
        RuleConfigService.class
})
class RuleConfigIntegrationTestConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

@Configuration
class RuleConfigListenerTestBeans {

    @Bean
    RuleConfigChangeListener ruleConfigChangeListener(RuleConfigService configService) {
        return new RuleConfigChangeListener(configService);
    }
}
