package cn.sztu.questioncloud.infrastructure.common.cache.config;

import lombok.Data;
import org.redisson.api.RateType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 缓存模块配置属性
 *
 * @author LinSanQi
 */
@Data
@Component
@ConfigurationProperties(prefix = "nexus.cache")
public class CacheProperties {

    /**
     * 布隆过滤器默认配置
     */
    private BloomFilter bloomFilter = new BloomFilter();

    /**
     * 令牌桶默认配置
     */
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();

    @Data
    public static class BloomFilter {
        /**
         * 预期插入的元素数量
         */
        private long expectedInsertions = 10000L;
        /**
         * 可接受的误判率
         */
        private double falseProbability = 0.01;
    }

    /**
     * RateLimiter 配置
     */
    @Data
    public static class RateLimiterConfig {
        /**
         * 限流器模式，默认：OVERALL
         */
        private RateType mode = RateType.OVERALL;
        /**
         * 速率
         */
        private long rate;
        /**
         * 速率间隔
         */
        private Duration rateInterval;
    }
} 