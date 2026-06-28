package com.ratelimiter.core.store;

import java.util.List;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import com.ratelimiter.core.config.RateLimitProperties;
import com.ratelimiter.core.dtos.RateLimitResult;
import com.ratelimiter.core.dtos.RateLimitRule;
import com.ratelimiter.core.utils.RuleLimitResolver;

public class RedisRateLimitStore implements RateLimitStore {
    
    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List<Long>> tokenBucketScript;
    private final RedisScript<List<Long>> slidingWindowCounterScript;
    private final RateLimitProperties.FailureMode failureMode;


    public RedisRateLimitStore(
        StringRedisTemplate redisTemplate,
        RedisScript<List<Long>> tokenBucketScript,
        RedisScript<List<Long>> slidingWindowCounterScript,
        RateLimitProperties properties) {
    this.redisTemplate = redisTemplate;
    this.tokenBucketScript = tokenBucketScript;
    this.slidingWindowCounterScript = slidingWindowCounterScript;
    this.failureMode = properties.getFailureMode();
}
@Override
public RateLimitResult allow(String scopeKey, RateLimitRule rule) {

    String redisKey = RateLimitKeyBuilder.redisKey(rule.getName(), scopeKey);

    try {
        List<Long> result = switch (rule.getAlgorithm().toLowerCase()) {
            case "token" -> executeTokenBucket(redisKey, rule);
            case "sliding-counter" -> executeSlidingWindow(redisKey, rule);
            default -> throw new IllegalArgumentException(
                    "Algorithm not supported in Redis store: " + rule.getAlgorithm());
        };
        return toRateLimitResult(result, rule);
    } catch (RedisConnectionFailureException ex) {
        return handleRedisFailure(ex);
    } catch (RateLimitStoreException ex) {
        throw ex;
    } catch (RuntimeException ex) {
        if (failureMode == RateLimitProperties.FailureMode.OPEN) {
            return RateLimitResult.of(true, Long.MAX_VALUE, 0);
        }
        throw new RateLimitStoreException("Redis rate limit check failed", ex);
    }
}

private List<Long> executeTokenBucket(String redisKey, RateLimitRule rule) {
    long ttlSeconds = Math.max(1, (rule.getWindowMillis() * 2) / 1000);
    if (ttlSeconds == 1 && rule.getWindowMillis() == 0) {
        ttlSeconds = 120;
    }

    return redisTemplate.execute(
            tokenBucketScript,
            List.of(redisKey),
            String.valueOf(rule.getCapacity()),
            String.valueOf(rule.getRefillPerSecond()),
            "1",
            String.valueOf(ttlSeconds));
}

private List<Long> executeSlidingWindow(String redisKey, RateLimitRule rule) {
    long ttlSeconds = Math.max(1, (rule.getWindowMillis() * 2) / 1000);

    return redisTemplate.execute(
            slidingWindowCounterScript,
            List.of(redisKey),
            String.valueOf(rule.getMaxRequests()),
            String.valueOf(rule.getWindowMillis()),
            String.valueOf(ttlSeconds));
}

private RateLimitResult toRateLimitResult(List<Long> result, RateLimitRule rule) {
    if (result == null || result.size() < 3) {
        throw new RateLimitStoreException("Unexpected Lua script result: " + result, null);
    }
    boolean allowed = result.get(0) == 1L;
    long remaining = result.get(1);
    long retryAfterMillis = result.get(2);
    long limit = RuleLimitResolver.resolveLimit(rule);
    long resetAt = retryAfterMillis > 0
            ? (System.currentTimeMillis() + retryAfterMillis) / 1000
            : (System.currentTimeMillis() / 1000) + (rule.getWindowMillis() / 1000);
  return new RateLimitResult(allowed, remaining, retryAfterMillis, limit, resetAt);
}

private RateLimitResult handleRedisFailure(RedisConnectionFailureException ex) {
    if (failureMode == RateLimitProperties.FailureMode.OPEN) {
        return RateLimitResult.of(true, Long.MAX_VALUE, 0);
    }
    throw new RateLimitStoreException("Redis is unavailable", ex);
}
}