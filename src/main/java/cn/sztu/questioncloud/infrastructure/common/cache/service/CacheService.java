package cn.sztu.questioncloud.infrastructure.common.cache.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 通用缓存服务接口，定义了对各种数据结构的缓存操作。
 *
 * @author <a href="https://github.com/lin037">lin037</a>
 */
public interface CacheService {

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否设置成功
     */
    <T> boolean set(String key, T value);

    /**
     * 设置缓存并指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    <T> boolean set(String key, T value, long timeout, TimeUnit unit);

    /**
     * 如果键不存在，则设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     * @return 是否设置成功
     */
    <T> boolean setIfAbsent(String key, T value);

    /**
     * 如果键不存在，则设置缓存并指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    <T> boolean setIfAbsent(String key, T value, long timeout, TimeUnit unit);

    /**
     * 获取缓存
     *
     * @param key 缓存键
     * @return 缓存值，不存在时返回null
     */
    <T> T get(String key);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean delete(String key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     * @return 成功删除的数量
     */
    long delete(List<String> keys);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置缓存过期时间
     *
     * @param key     缓存键
     * @param timeout 过期时间
     * @param unit    时间单位
     * @return 是否设置成功
     */
    boolean expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取缓存过期时间
     *
     * @param key  缓存键
     * @param unit 时间单位
     * @return 过期时间，-2表示键不存在，-1表示永不过期
     */
    long getExpire(String key, TimeUnit unit);

    /**
     * 递增
     *
     * @param key 缓存键
     * @return 递增后的值
     */
    long increment(String key);

    /**
     * 递增指定数量
     *
     * @param key   缓存键
     * @param delta 增量
     * @return 递增后的值
     */
    long increment(String key, long delta);

    /**
     * 递减
     *
     * @param key 缓存键
     * @return 递减后的值
     */
    long decrement(String key);

    /**
     * 递减指定数量
     *
     * @param key   缓存键
     * @param delta 减量
     * @return 递减后的值
     */
    long decrement(String key, long delta);

    /**
     * 获取符合模式的所有键
     *
     * @param pattern 模式
     * @return 键集合
     */
    Set<String> keys(String pattern);

    // Hash操作

    /**
     * 设置Hash缓存
     *
     * @param key   缓存键
     * @param field Hash字段
     * @param value Hash值
     * @return 是否设置成功
     */
    <T> boolean hashSet(String key, String field, T value);

    /**
     * 批量设置Hash缓存
     *
     * @param key 缓存键
     * @param map Hash映射
     * @return 是否设置成功
     */
    <T> boolean hashSetAll(String key, Map<String, T> map);

    /**
     * 获取Hash缓存
     *
     * @param key   缓存键
     * @param field Hash字段
     * @return Hash值
     */
    <T> T hashGet(String key, String field);

    /**
     * 获取Hash中的所有值
     *
     * @param key 缓存键
     * @return Hash值映射
     */
    <T> Map<String, T> hashGetAll(String key);

    /**
     * 删除Hash字段
     *
     * @param key    缓存键
     * @param fields Hash字段
     * @return 成功删除的数量
     */
    long hashDelete(String key, String... fields);

    /**
     * 判断Hash字段是否存在
     *
     * @param key   缓存键
     * @param field Hash字段
     * @return 是否存在
     */
    boolean hashExists(String key, String field);

    /**
     * 获取Hash大小
     *
     * @param key 缓存键
     * @return Hash大小
     */
    long hashSize(String key);

    // List操作

    /**
     * 将值插入List左端
     *
     * @param key   缓存键
     * @param value 值
     * @return List长度
     */
    <T> long listLeftPush(String key, T value);

    /**
     * 将值插入List右端
     *
     * @param key   缓存键
     * @param value 值
     * @return List长度
     */
    <T> long listRightPush(String key, T value);

    /**
     * 获取List中指定范围的元素
     *
     * @param key   缓存键
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    <T> List<T> listRange(String key, long start, long end);

    /**
     * 获取List长度
     *
     * @param key 缓存键
     * @return List长度
     */
    long listSize(String key);

    /**
     * 从List左端弹出元素
     *
     * @param key 缓存键
     * @return 弹出的元素，如果List为空或不存在返回null
     */
    <T> T listLeftPop(String key);

    /**
     * 从List右端弹出元素
     *
     * @param key 缓存键
     * @return 弹出的元素，如果List为空或不存在返回null
     */
    <T> T listRightPop(String key);

    // Set操作

    /**
     * 将值添加到Set
     *
     * @param key    缓存键
     * @param values 值集合
     * @return 添加的数量
     */
    <T> long setAdd(String key, Collection<T> values);

    /**
     * 获取Set中的所有值
     *
     * @param key 缓存键
     * @return 值集合
     */
    <T> Set<T> setMembers(String key);

    /**
     * 判断值是否是Set的成员
     *
     * @param key   缓存键
     * @param value 值
     * @return 是否是成员
     */
    <T> boolean setIsMember(String key, T value);

    /**
     * 获取Set大小
     *
     * @param key 缓存键
     * @return Set大小
     */
    long setSize(String key);

    /**
     * 从Set中删除值
     *
     * @param key    缓存键
     * @param values 值集合
     * @return 删除的数量
     */
    <T> long setRemove(String key, Collection<T> values);

    /**
     * 关闭缓存资源
     * 清理缓存实现相关的资源
     */
    void close();
} 