package cn.sztu.questioncloud.infrastructure.adapter.user;

import cn.sztu.questioncloud.application.user.enums.VerificationTypeEnum;
import cn.sztu.questioncloud.application.user.port.UserVerificationPort;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.cache.util.CacheKeyBuilder;
import cn.sztu.questioncloud.infrastructure.common.exception.CommonInfraExceptionEnum;
import cn.sztu.questioncloud.infrastructure.common.exception.InfrastructureException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserVerificationAdapter implements UserVerificationPort {

    // 同一邮箱/标识两次发送验证码之间的最小间隔，单位：秒
    private static final long SINGLE_SEND_INTERVAL_SECONDS = 60;
    // 发送频控统计窗口，单位：分钟
    private static final long RATE_LIMIT_WINDOW_MINUTES = 30;
    // 在统计窗口内允许发送验证码的最大次数
    private static final int RATE_LIMIT_MAX_REQUESTS = 5;
    // 超过最大发送次数后的限制时长，单位：分钟
    private static final long BLOCK_DURATION_MINUTES = 30;

    private final SecureRandom random = new SecureRandom();
    private final CacheService cacheService;


    @Override
    public String createVerificationToken(String identify, VerificationTypeEnum type) {
        if (hasRateLimitReached(identify, type)) {
            throw new ApplicationException(
                    CommonResultCodeEnum.CANCEL,
                    "验证码发送过于频繁，请稍后再试"
            );
        }
        //获取 key 并设置冷却期，防止短时间内重复发送
        String cooldownKey = getRateLimitCooldownKey(identify, type);
        boolean cooldownSet = cacheService.set(cooldownKey, "1", SINGLE_SEND_INTERVAL_SECONDS, TimeUnit.SECONDS);
        if (!cooldownSet) {
            throw new InfrastructureException(
                    CommonInfraExceptionEnum.INFRA_COMMON_UNKNOWN_ERROR,
                    "验证码发送频控初始化失败"
            );
        }
        //增加发送计数器，统计窗口内的发送次数
        String counterKey = getRateLimitCounterKey(identify, type);
        long count = cacheService.increment(counterKey);
        if (count == 1) {
            boolean expired = cacheService.expire(counterKey, RATE_LIMIT_WINDOW_MINUTES, TimeUnit.MINUTES);
            if (!expired) {
                throw new InfrastructureException(
                        CommonInfraExceptionEnum.INFRA_COMMON_UNKNOWN_ERROR,
                        "验证码发送计数窗口初始化失败"
                );
            }
        }
        //生成验证码并保存到缓存，设置过期时间为验证码的有效期
        String code = generateCode();
        String codeKey = getVerificationCodeKey(identify, type);
        boolean codeSet = cacheService.set(codeKey, code, type.getExpiryMinutes(), TimeUnit.MINUTES);
        if (!codeSet) {
            throw new InfrastructureException(
                    CommonInfraExceptionEnum.INFRA_COMMON_UNKNOWN_ERROR,
                    "验证码缓存失败"
            );
        }

        return code;
    }

    @Override
    public String getVerificationToken(String identify, VerificationTypeEnum type) {
        String codeKey = getVerificationCodeKey(identify, type);
        return cacheService.get(codeKey);
    }

    @Override
    public boolean verifyCode(String identify, String code, VerificationTypeEnum type) {
        String storedCode = getVerificationToken(identify, type);
        return code != null && code.equals(storedCode);
    }

    @Override
    public void deleteVerificationToken(String identify, VerificationTypeEnum type) {
        String codeKey = getVerificationCodeKey(identify, type);
        boolean deleted = cacheService.delete(codeKey);
        if (!deleted && cacheService.exists(codeKey)) {
            throw new InfrastructureException(
                    CommonInfraExceptionEnum.INFRA_COMMON_UNKNOWN_ERROR,
                    "验证码删除失败"
            );
        }
    }

    @Override
    public boolean hasRateLimitReached(String identify, VerificationTypeEnum type) {
        // 1. 检查是否已被封禁
        if (cacheService.exists(getRateLimitBlockKey(identify, type))) {
            return true;
        }
        // 2. 检查是否处于 1 分钟冷却中
        if (cacheService.exists(getRateLimitCooldownKey(identify, type))) {
            return true;
        }
        // 3. 检查 30 分钟窗口内的发送次数
        String windowKey = getRateLimitCounterKey(identify, type);
        Long count = cacheService.get(windowKey);
        if (count != null && count >= RATE_LIMIT_MAX_REQUESTS) {
            String blockKey = getRateLimitBlockKey(identify, type);
            boolean blockSet = cacheService.set(blockKey, "1", BLOCK_DURATION_MINUTES, TimeUnit.MINUTES);
            if (!blockSet) {
                throw new InfrastructureException(
                        CommonInfraExceptionEnum.INFRA_COMMON_UNKNOWN_ERROR,
                        "验证码封禁状态设置失败"
                );
            }
            cacheService.delete(windowKey);
            return true;
        }

        return false;
    }

    //==== 私有辅助方法 ==//

    /**
     * 生成6位数字验证码
     */
    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    // 存入验证码内容的缓存 key
    private String getVerificationCodeKey(String identify, VerificationTypeEnum type) {
        return CacheKeyBuilder.buildKey("user", "verification", "code", identify, type.name().toLowerCase());
    }
    //短时间冷却状态 key, "rate_limit"默认为 1
    private String getRateLimitCooldownKey(String identify, VerificationTypeEnum type) {
        return CacheKeyBuilder.buildKey("user", "verification", "rate_limit", "minute", identify, type.name().toLowerCase());
    }
    //较长时间窗口里的发送次数统计 key
    private String getRateLimitCounterKey(String identify, VerificationTypeEnum type) {
        return CacheKeyBuilder.buildKey("user", "verification", "rate_limit", "counter", identify, type.name().toLowerCase());
    }
    //封禁状态 key, "block"默认为 1
    private String getRateLimitBlockKey(String identify, VerificationTypeEnum type) {
        return CacheKeyBuilder.buildKey("user", "verification", "rate_limit", "block", identify, type.name().toLowerCase());
    }
}
