package com.ratelimiter.core.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.ratelimiter.core.dtos.RateLimitRule;

@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private RateLimitStoreType store = RateLimitStoreType.MEMORY;
    private FailureMode failureMode = FailureMode.CLOSED;
    private Redis redis = new Redis();
    private List<RateLimitRule> rules = new ArrayList<>();

    public RateLimitStoreType getStore() {
        return store;
    }

    public void setStore(RateLimitStoreType store) {
        this.store = store;
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public void setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
    }

    public Redis getRedis() {
        return redis;
    }

    public void setRedis(Redis redis) {
        this.redis = redis;
    }

    public List<RateLimitRule> getRules() {
        return rules;
    }

    public void setRules(List<RateLimitRule> rules) {
        this.rules = rules;
    }

    public enum FailureMode {
        CLOSED,
        OPEN
    }

    public static class Redis {
        private String host = "localhost";
        private int port = 6379;
        private long timeoutMillis = 50;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }
}
