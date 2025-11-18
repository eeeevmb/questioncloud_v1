package cn.sztu.questioncloud.infrastructure.common.cache.service.impl;

import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.redisson.api.options.KeysScanOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Redisson 缓存服务实现。
 * 提供了对 Redis 的各种数据结构的完整操作。
 *
 * @author <a href="https://github.com/lin037">lin037</a>
 */
@Component("RedissonCacheService")
@RequiredArgsConstructor
public class RedissonCacheService implements CacheService {

    /**
     * Redisson 客户端实例，用于与 Redis 服务器进行交互。
     */
    private final RedissonClient redissonClient;

    // String operations

    /**
     * 设置一个字符串类型的键值对。
     * 使用 RBucket 接口进行基本的 key-value 存储。
     * 操作成功返回 true，异常捕获后返回 false。
     *
     * @param key   缓存的键
     * @param value 缓存的值
     * @return 是否设置成功
     */
    @Override
    public <T> boolean set(String key, T value) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            bucket.set(value);
            return true;
        } catch (Exception e) {
            // Log exception here
            return false;
        }
    }

    /**
     * 设置带过期时间的字符串型缓存。
     * 使用 Duration 来替代旧版 TimeUnit，提高可读性和兼容性。
     *
     * @param key     缓存的键
     * @param value   缓存的值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public <T> boolean set(String key, T value, long timeout, TimeUnit unit) {
        try {
            RBucket<T> bucket = redissonClient.getBucket(key);
            bucket.set(value, Duration.of(timeout, unit.toChronoUnit()));
            return true;
        } catch (Exception e) {
            // Log exception here
            return false;
        }
    }

    /**
     * 仅当键不存在时才设置缓存（setIfAbsent）。
     *
     * @param key   缓存的键
     * @param value 缓存的值
     * @return 是否设置成功（如果键已存在则返回 false）
     */
    @Override
    public <T> boolean setIfAbsent(String key, T value) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value);
    }

    /**
     * 带过期时间的 setIfAbsent 方法。
     *
     * @param key     缓存的键
     * @param value   缓存的值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public <T> boolean setIfAbsent(String key, T value, long timeout, TimeUnit unit) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.setIfAbsent(value, Duration.of(timeout, unit.toChronoUnit()));
    }

    /**
     * 获取指定键对应的值。
     *
     * @param key 缓存的键
     * @return 缓存的值，若不存在则返回 null
     */
    @Override
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        return bucket.get();
    }

    /**
     * 删除单个缓存键。
     *
     * @param key 要删除的键
     * @return 是否删除成功
     */
    @Override
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 批量删除缓存键。
     * 使用 RKeys.delete 方法一次删除多个键。
     *
     * @param keys 要删除的键列表
     * @return 删除成功的键数量
     */
    @Override
    public long delete(List<String> keys) {
        RKeys rKeys = redissonClient.getKeys();
        return rKeys.delete(keys.toArray(new String[0]));
    }

    /**
     * 判断某个键是否存在。
     *
     * @param key 要检查的键
     * @return 键是否存在
     */
    @Override
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 设置键的过期时间。
     *
     * @param key     要设置的键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    @Override
    public boolean expire(String key, long timeout, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        return bucket.expire(Duration.of(timeout, unit.toChronoUnit()));
    }

    /**
     * 获取键的剩余生存时间（TTL）。
     *
     * @param key  要查询的键
     * @param unit 返回的时间单位
     * @return 剩余时间（按 unit 单位），若未设置过期则返回 -1
     */
    @Override
    public long getExpire(String key, TimeUnit unit) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        long ttlMillis = bucket.remainTimeToLive();
        if (ttlMillis > 0) {
            return unit.convert(ttlMillis, TimeUnit.MILLISECONDS);
        }
        return ttlMillis;
    }

    /**
     * 自增操作，默认增量为 1。
     *
     * @param key 键名
     * @return 自增后的结果
     */
    @Override
    public long increment(String key) {
        return increment(key, 1L);
    }

    /**
     * 自定义增量的自增操作。
     *
     * @param key   键名
     * @param delta 增量
     * @return 自增后的结果
     */
    @Override
    public long increment(String key, long delta) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object value = bucket.get();
        long newValue;
        if (value instanceof Number) {
            newValue = ((Number) value).longValue() + delta;
        } else if (value instanceof String) {
            newValue = Long.parseLong((String) value) + delta;
        } else {
            // 如果 key 不存在或类型不兼容，则从 delta 开始
            newValue = delta;
        }
        bucket.set(newValue);
        return newValue;
    }

    /**
     * 自减操作，默认减量为 1。
     *
     * @param key 键名
     * @return 自减后的结果
     */
    @Override
    public long decrement(String key) {
        return decrement(key, 1L);
    }

    /**
     * 自定义减量的自减操作。
     *
     * @param key   键名
     * @param delta 减量
     * @return 自减后的结果
     */
    @Override
    public long decrement(String key, long delta) {
        RBucket<Object> bucket = redissonClient.getBucket(key);
        Object value = bucket.get();
        long newValue;
        if (value instanceof Number) {
            newValue = ((Number) value).longValue() - delta;
        } else if (value instanceof String) {
            newValue = Long.parseLong((String) value) - delta;
        } else {
            // 如果 key 不存在或类型不兼容，则从 -delta 开始
            newValue = -delta;
        }
        bucket.set(newValue);
        return newValue;
    }

    /**
     * 根据匹配模式获取所有键。
     * 使用 KeysScanOptions 构建扫描选项，并通过 pattern 匹配。
     *
     * @param pattern 匹配模式（如 "user:*"）
     * @return 匹配的所有键集合
     */
    @Override
    public Set<String> keys(String pattern) {
        RKeys rKeys = redissonClient.getKeys();
        KeysScanOptions options = KeysScanOptions.defaults();
        options.pattern(pattern); // 设置匹配模式
        Iterable<String> keysIterable = rKeys.getKeys(options);
        return StreamSupport.stream(keysIterable.spliterator(), false)
                .collect(Collectors.toSet());
    }


    // Hash operations

    /**
     * 将哈希表 key 中域 field 的值设为 value。
     *
     * @param key   缓存键
     * @param field 哈希域
     * @param value 值
     * @param <T>   值的类型
     * @return 操作是否成功
     */
    @Override
    public <T> boolean hashSet(String key, String field, T value) {
        RMap<String, T> map = redissonClient.getMap(key);
        map.put(field, value);
        return true;
    }

    /**
     * 同时将多个 field-value (域-值)对设置到哈希表 key 中。
     *
     * @param key 缓存键
     * @param map 包含多个域和值的映射
     * @param <T> 值的类型
     * @return 操作是否成功
     */
    @Override
    public <T> boolean hashSetAll(String key, Map<String, T> map) {
        RMap<String, T> rMap = redissonClient.getMap(key);
        rMap.putAll(map);
        return true;
    }

    /**
     * 获取存储在哈希表中指定域的值。
     *
     * @param key   缓存键
     * @param field 哈希域
     * @param <T>   值的类型
     * @return 域的值，如果域不存在或键不存在则返回 null
     */
    @Override
    public <T> T hashGet(String key, String field) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.get(field);
    }

    /**
     * 获取在哈希表中指定 key 的所有域和值。
     *
     * @param key 缓存键
     * @param <T> 值的类型
     * @return 包含所有域和值的映射
     */
    @Override
    public <T> Map<String, T> hashGetAll(String key) {
        RMap<String, T> map = redissonClient.getMap(key);
        return map.readAllMap();
    }

    /**
     * 删除哈希表 key 中的一个或多个指定域。
     *
     * @param key    缓存键
     * @param fields 要删除的哈希域
     * @return 被成功删除的域的数量
     */
    @Override
    public long hashDelete(String key, String... fields) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        return map.fastRemove((Object[]) fields);
    }

    /**
     * 查看哈希表 key 中，指定的域是否存在。
     *
     * @param key   缓存键
     * @param field 哈希域
     * @return 如果哈希表含有给定域，返回 true
     */
    @Override
    public boolean hashExists(String key, String field) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        return map.containsKey(field);
    }

    /**
     * 获取哈希表中的域数量。
     *
     * @param key 缓存键
     * @return 域的数量
     */
    @Override
    public long hashSize(String key) {
        RMap<Object, Object> map = redissonClient.getMap(key);
        return map.size();
    }

    // List operations

    /**
     * 将一个值插入到列表头部。
     *
     * @param key   缓存键
     * @param value 要插入的值
     * @param <T>   值的类型
     * @return 执行操作后列表的长度
     */
    @Override
    public <T> long listLeftPush(String key, T value) {
        RList<T> list = redissonClient.getList(key);
        list.addFirst(value);
        return list.size();
    }

    /**
     * 将一个值插入到列表尾部。
     *
     * @param key   缓存键
     * @param value 要插入的值
     * @param <T>   值的类型
     * @return 执行操作后列表的长度
     */
    @Override
    public <T> long listRightPush(String key, T value) {
        RList<T> list = redissonClient.getList(key);
        list.add(value);
        return list.size();
    }

    /**
     * 获取列表指定范围内的元素。
     *
     * @param key   缓存键
     * @param start 起始索引
     * @param end   结束索引
     * @param <T>   元素的类型
     * @return 指定范围内的元素列表
     */
    @Override
    public <T> List<T> listRange(String key, long start, long end) {
        RList<T> list = redissonClient.getList(key);
        return list.range((int) start, (int) end);
    }

    /**
     * 获取列表的长度。
     *
     * @param key 缓存键
     * @return 列表的长度
     */
    @Override
    public long listSize(String key) {
        RList<Object> list = redissonClient.getList(key);
        return list.size();
    }

    /**
     * 移出并获取列表的第一个元素。
     *
     * @param key 缓存键
     * @param <T> 元素的类型
     * @return 列表的第一个元素，如果列表为空则返回 null
     */
    @Override
    public <T> T listLeftPop(String key) {
        RList<T> list = redissonClient.getList(key);
        if (list.isEmpty()) {
            return null;
        }
        return list.removeFirst();
    }

    /**
     * 移出并获取列表的最后一个元素。
     *
     * @param key 缓存键
     * @param <T> 元素的类型
     * @return 列表的最后一个元素，如果列表为空则返回 null
     */
    @Override
    public <T> T listRightPop(String key) {
        RList<T> list = redissonClient.getList(key);
        if (list.isEmpty()) {
            return null;
        }
        return list.removeLast();
    }

    // Set operations

    /**
     * 向集合添加一个或多个成员。
     *
     * @param key    缓存键
     * @param values 要添加的成员集合
     * @param <T>    成员的类型
     * @return 成功添加的新成员数量
     */
    @Override
    public <T> long setAdd(String key, Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        RScript script = redissonClient.getScript();
        return script.eval(RScript.Mode.READ_WRITE, "return redis.call('sadd', KEYS[1], unpack(ARGV))", RScript.ReturnType.INTEGER, Collections.singletonList(key), values.toArray());
    }

    /**
     * 获取集合的所有成员。
     *
     * @param key 缓存键
     * @param <T> 成员的类型
     * @return 集合中的所有成员
     */
    @Override
    public <T> Set<T> setMembers(String key) {
        RSet<T> set = redissonClient.getSet(key);
        return set.readAll();
    }

    /**
     * 判断成员元素是否是集合的成员。
     *
     * @param key   缓存键
     * @param value 要判断的成员
     * @param <T>   成员的类型
     * @return 如果成员是集合的成员，返回 true
     */
    @Override
    public <T> boolean setIsMember(String key, T value) {
        RSet<T> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    /**
     * 获取集合的成员数。
     *
     * @param key 缓存键
     * @return 集合的成员数
     */
    @Override
    public long setSize(String key) {
        RSet<Object> set = redissonClient.getSet(key);
        return set.size();
    }

    /**
     * 移除集合中一个或多个成员。
     *
     * @param key    缓存键
     * @param values 要移除的成员集合
     * @param <T>    成员的类型
     * @return 被成功移除的成员的数量
     */
    @Override
    public <T> long setRemove(String key, Collection<T> values) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        RScript script = redissonClient.getScript();
        return script.eval(RScript.Mode.READ_WRITE, "return redis.call('srem', KEYS[1], unpack(ARGV))", RScript.ReturnType.INTEGER, Collections.singletonList(key), values.toArray());
    }

    /**
     * 关闭 Redisson 客户端连接。
     * 在应用关闭时调用此方法释放资源。
     */
    @Override
    public void close() {
        if (redissonClient != null && !redissonClient.isShutdown()) {
            redissonClient.shutdown();
        }
    }

}
