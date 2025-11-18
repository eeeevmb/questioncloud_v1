package cn.sztu.questioncloud.infrastructure.common.cache.service;

import java.util.List;

/**
 * 布隆过滤器服务接口
 *
 * @author LinSanQi
 */
public interface BloomFilterService {

    /**
     * 向布隆过滤器中添加一个元素。
     *
     * @param key     过滤器键
     * @param element 要添加的元素
     * @return 如果元素被成功添加，则为 true；如果元素可能已存在，则为 false
     */
    <T> boolean add(String key, T element);

    /**
     * 向布隆过滤器中添加多个元素。
     *
     * @param key      过滤器键
     * @param elements 要添加的元素列表
     * @return 添加的元素数量
     */
    <T> long addAll(String key, List<T> elements);

    /**
     * 检查一个元素是否存在于布隆过滤器中。
     *
     * @param key     过滤器键
     * @param element 要检查的元素
     * @return 如果元素可能存在，则为 true；如果元素绝对不存在，则为 false
     */
    <T> boolean contains(String key, T element);

    /**
     * 尝试初始化布隆过滤器。
     * 如果过滤器已存在，则返回 true。
     * 如果过滤器不存在，则创建一个新的过滤器。
     *
     * @param key                过滤器键
     * @param expectedInsertions 预期插入的元素数量
     * @param falseProbability   可接受的误判率
     */
    boolean tryInit(String key, long expectedInsertions, double falseProbability);

    /**
     * 删除布隆过滤器。
     *
     * @param key 过滤器键
     * @return 如果删除成功返回true
     */
    boolean delete(String key);

} 