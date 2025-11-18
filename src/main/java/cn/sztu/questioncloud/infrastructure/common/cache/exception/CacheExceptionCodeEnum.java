package cn.sztu.questioncloud.infrastructure.common.cache.exception;

import cn.sztu.questioncloud.infrastructure.common.exception.InfraExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缓存服务相关的异常码枚举
 *
 * @author LinSanQi
 */
@Getter
@AllArgsConstructor
public enum CacheExceptionCodeEnum implements InfraExceptionCode {

    LUA_SCRIPT_EXECUTION_ERROR("INFRA_CACHE_LUA_SCRIPT_ERROR", "Lua脚本执行失败"),
    BLOOM_FILTER_INIT_FAILED("INFRA_CACHE_BLOOM_FILTER_INIT_FAILED", "布隆过滤器初始化失败"),
    RATE_LIMITER_INIT_FAILED("INFRA_CACHE_RATE_LIMITER_INIT_FAILED", "令牌桶初始化失败");

    private final String code;
    private final String message;
} 