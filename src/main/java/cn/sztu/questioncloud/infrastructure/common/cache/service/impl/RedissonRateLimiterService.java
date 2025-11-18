package cn.sztu.questioncloud.infrastructure.common.cache.service.impl;

import cn.sztu.questioncloud.infrastructure.common.cache.service.RateLimiterService;
import cn.sztu.questioncloud.infrastructure.common.cache.util.CacheKeyBuilder;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redisson 分布式限流器服务实现
 *
 * @author <a href="https://github.com/lin037">lin037</a>
 * @date 2024/7/25 0:24
 */
@Service
@RequiredArgsConstructor
public class RedissonRateLimiterService implements RateLimiterService {

    private final RedissonClient redissonClient;

    @Override
    public boolean isAllowed(String key) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        return rateLimiter.tryAcquire();
    }

    @Override
    public boolean trySetRate(String key, RateType mode, long rate, Duration rateInterval) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        return rateLimiter.trySetRate(mode, rate, rateInterval);
    }

    @Override
    public boolean tryAcquire(String key) {
        return tryAcquire(key, 1);
    }

    @Override
    public boolean tryAcquire(String key, int permits) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        return rateLimiter.tryAcquire(permits);
    }

    @Override
    public boolean delete(String key) {
        RRateLimiter rateLimiter = getRateLimiter(key);
        return rateLimiter.delete();
    }

    private RRateLimiter getRateLimiter(String key) {
        String finalKey = CacheKeyBuilder.buildKey("rate-limiter", key);
        return redissonClient.getRateLimiter(finalKey);
    }
} 