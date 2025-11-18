package cn.sztu.questioncloud.infrastructure.common.cache.util;


import cn.sztu.questioncloud.common.constant.SystemConstant;

/**
 * 缓存Key构建工具类
 *
 * @author LinSanQi
 */
public final class CacheKeyBuilder {

    private CacheKeyBuilder() {
    }

    /**
     * 构建一个完整的Redis Key
     * 格式: SYSTEM_NAME:module:key1:key2...
     *
     * @param module 模块名 (例如: user, product)
     * @param keys   具体的键名部分
     * @return 构建好的Redis Key
     */
    public static String buildKey(String module, String... keys) {
        if (module == null || module.isBlank()) {
            throw new IllegalArgumentException("模块名不能为空");
        }
        String modulePart = SystemConstant.buildRedisKey(module);
        if (keys == null || keys.length == 0) {
            return modulePart;
        }
        return SystemConstant.appendToKey(modulePart, keys);
    }
} 