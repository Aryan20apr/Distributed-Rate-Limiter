package com.ratelimiter.core.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import com.ratelimiter.core.service.RuleConfigChangeListener;
import com.ratelimiter.core.repositories.RuleRepository;

@Configuration
@ConditionalOnProperty(prefix = "rate-limit", name = "store", havingValue = "redis")
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RuleConfigChangeListener listener) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(
                listener,
                new ChannelTopic(RuleRepository.CHANGED_CHANNEL));
        return container;
    } 

    @Bean
    RedisConnectionFactory redisConnectionFactory(RateLimitProperties properties) {
        RateLimitProperties.Redis redis = properties.getRedis();

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(
                redis.getHost(), redis.getPort());

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(redis.getTimeoutMillis()))
                .build();

        return new LettuceConnectionFactory(standalone, clientConfig);
    }

    @Bean
    StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        return template;
    }

    @Bean
    RedisScript<List<Long>> tokenBucketScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/token_bucket.lua"));
        script.setResultType(listLongType());
        return script;
    }

    @Bean
    RedisScript<List<Long>> slidingWindowCounterScript() {
        DefaultRedisScript<List<Long>> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("lua/sliding_window.lua"));
        script.setResultType(listLongType());
        return script;
    }

    @SuppressWarnings("unchecked")
    private static Class<List<Long>> listLongType() {
        return (Class<List<Long>>) (Class<?>) List.class;
    }
}
