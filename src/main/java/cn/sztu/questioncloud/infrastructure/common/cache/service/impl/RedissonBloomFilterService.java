//package cn.sztu.questioncloud.infrastructure.common.cache.service.impl;
//
//import cn.lin037.nexus.infrastructure.common.cache.exception.CacheExceptionCodeEnum;
//import cn.lin037.nexus.infrastructure.common.cache.service.BloomFilterService;
//import cn.lin037.nexus.infrastructure.common.cache.util.CacheKeyBuilder;
//import cn.lin037.nexus.infrastructure.common.exception.InfrastructureException;
//import lombok.RequiredArgsConstructor;
//import org.redisson.api.RBloomFilter;
//import org.redisson.api.RedissonClient;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * Redisson 布隆过滤器服务实现
// *
// * @author <a href="https://github.com/lin037">lin037</a>
// * @date 2024/7/25 0:24
// */
//@Service
//@RequiredArgsConstructor
//public class RedissonBloomFilterService implements BloomFilterService {
//
//    private final RedissonClient redissonClient;
//
//    @Override
//    public <T> boolean add(String key, T element) {
//        // 过滤器元素不能为空
//        if (element == null) return false;
//        RBloomFilter<T> bloomFilter = getBloomFilter(key);
//        return bloomFilter.add(element);
//    }
//
//    public <R> long addAll(String key, List<R> elements) {
//        // 集合元素不能为空
//        if (elements == null || elements.isEmpty()) return 0;
//        RBloomFilter<R> bloomFilter = getBloomFilter(key);
//        return bloomFilter.add(elements);
//    }
//
//    @Override
//    public <T> boolean contains(String key, T element) {
//        RBloomFilter<T> bloomFilter = getBloomFilter(key);
//        return bloomFilter.contains(element);
//    }
//
//    @Override
//    public boolean tryInit(String key, long expectedInsertions, double falseProbability) {
//        if (expectedInsertions <= 0 || falseProbability <= 0 || falseProbability >= 1) {
//            throw new InfrastructureException(CacheExceptionCodeEnum.BLOOM_FILTER_INIT_FAILED);
//        }
//        RBloomFilter<Object> bloomFilter = getBloomFilter(key);
//        if (bloomFilter.isExists()) {
//            return true;
//        }
//        return bloomFilter.tryInit(expectedInsertions, falseProbability);
//    }
//
//    @Override
//    public boolean delete(String key) {
//        RBloomFilter<Object> bloomFilter = getBloomFilter(key);
//        return bloomFilter.delete();
//    }
//
//    private <T> RBloomFilter<T> getBloomFilter(String key) {
//        String finalKey = CacheKeyBuilder.buildKey("bloom-filter", key);
//        return redissonClient.getBloomFilter(finalKey);
//    }
//}