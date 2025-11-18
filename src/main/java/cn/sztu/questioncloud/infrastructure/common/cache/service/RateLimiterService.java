package cn.sztu.questioncloud.infrastructure.common.cache.service;

import org.redisson.api.RateType;

import java.time.Duration;

/**
 * 分布式限流器服务接口
 *
 * @author <a href="https://github.com/lin037">lin037</a>
 * @date 2024/7/25 0:21
 */
public interface RateLimiterService {

    /**
     * 检查指定键的请求是否被允许。
     *
     * @param key 限流器键
     * @return 如果请求在速率限制内，则为 true；否则为 false
     */
    boolean isAllowed(String key);

    /**
     * 尝试设置速率限制器。仅当速率限制器尚未设置时才会成功。
     *
     * @param key          限流器键
     * @param mode         速率模式
     * @param rate         速率
     * @param rateInterval 速率时间间隔
     * @return 如果成功设置了速率，则为 true；否则为 false
     */
    boolean trySetRate(String key, RateType mode, long rate, Duration rateInterval);

    /**
     * 尝试获取一个许可。
     *
     * @param key 限流器键
     * @return 如果成功获取返回true，否则返回false
     */
    boolean tryAcquire(String key);

    /**
     * 尝试获取指定数量的许可。
     *
     * @param key     限流器键
     * @param permits 要获取的许可数量
     * @return 如果成功获取返回true，否则返回false
     */
    boolean tryAcquire(String key, int permits);

    /**
     * 删除限流器。
     *
     * @param key 限流器键
     * @return 如果删除成功返回true
     */
    boolean delete(String key);

} 