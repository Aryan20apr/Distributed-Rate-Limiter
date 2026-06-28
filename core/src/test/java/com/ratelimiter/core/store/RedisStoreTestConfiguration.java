package com.ratelimiter.core.store;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.config.RedisConfig;

@SpringBootConfiguration
@EnableConfigurationProperties(RateLimitProperties.class)
@Import(RedisConfig.class)
class RedisStoreTestConfiguration {
}
