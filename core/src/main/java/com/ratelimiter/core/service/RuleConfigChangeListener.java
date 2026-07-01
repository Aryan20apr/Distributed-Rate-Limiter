package com.ratelimiter.core.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@ConditionalOnBean(RuleConfigService.class)
@Slf4j
public class RuleConfigChangeListener implements MessageListener {

    private final RuleConfigService configService;

    public RuleConfigChangeListener(RuleConfigService configService) {
        this.configService = configService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String ruleName = new String(message.getBody());
        log.info("Rule config change notification: {}", ruleName);
        configService.reloadRule(ruleName);
    }
}