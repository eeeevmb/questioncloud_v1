package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.CollectionSourceEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.port.CollectionPresenceCheckerPort;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.application.question.service.QuestionCollectionService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 题目模块应用服务实现
 *
 * @author eeeevmb
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionCollectionServiceImpl implements QuestionCollectionService {
    private final CollectionPresenceCheckerPort collectionPresenceCheckerPort;
    private final QuestionCollectionRepository questionCollectionRepository;
    private final CacheService cacheService;

    public static final Long CACHE_TTL_HOURS = 4L;
    /**
     * 创建默认题集
     * 用户注册后调用一次
     *
     * @param userId 用户id
     */
    @Override
    public void createDefaultCollection(Long userId) {
        // 1. 检查是否有默认题集
        if (collectionPresenceCheckerPort.existsDefaultByUserId(userId)) {
            return;
        }
        // 2. 创建默认题集
        QuestionCollectionEntity defaultCollection = QuestionCollectionEntity.builder()
                .name("默认题集")
                .description("系统自动创建")
                .ownerId(userId)
                .source(CollectionSourceEnum.SYSTEM.getCode())
                .build();
        questionCollectionRepository.save(defaultCollection);
    }

    /**
     * 创建题集
     *
     * @param req 创建题集请求
     * @return 题集视图对象
     */
    @Override
    public CollectionVO createCollection(CreateCollectionReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        LocalDateTime now = LocalDateTime.now();

        ensureNameAvailable(userId, req.name());

        // 2. 构建题集实体
        QuestionCollectionEntity collection = QuestionCollectionEntity.builder()
                .id(HutoolSnowflakeIdGenerator.generateLongId())
                .name(req.name())
                .description(req.description())
                .ownerId(userId)
                .source(CollectionSourceEnum.USER.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();
        questionCollectionRepository.save(collection);

        String key = CacheKeyUtil.questionCollectionKey(userId);
        cacheService.delete(key);

        // 3. 返回视图对象
        return CollectionVO.fromEntity(collection);
    }

    @Override
    public List<CollectionVO> createCollections(List<CreateCollectionReq> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        Long userId = StpUtil.getLoginIdAsLong();
        ensureNamesAvailable(userId, reqs.stream().map(CreateCollectionReq::name).toList());

        List<QuestionCollectionEntity> collections = reqs.stream()
                .map(req -> QuestionCollectionEntity.builder()
                        .id(HutoolSnowflakeIdGenerator.generateLongId())
                        .name(req.name())
                        .description(req.description())
                        .ownerId(userId)
                        .source(CollectionSourceEnum.USER.getCode())
                        .createdAt(now)
                        .updatedAt(now)
                        .build())
                .toList();
        questionCollectionRepository.batchSave(collections);

        String key = CacheKeyUtil.questionCollectionKey(userId);
        cacheService.delete(key);

        return collections.stream()
                .map(CollectionVO::fromEntity)
                .toList();
    }

    /**
     * 更新题集
     *
     * @param req 更新题集请求
     * @return 题集视图对象
     */
    @Override
    public CollectionVO updateCollection(Long collectionId, UpdateCollectionReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 获取待更新的题集实体
        QuestionCollectionEntity entity =  questionCollectionRepository.findById(collectionId).orElseThrow(
                () -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND));

        // 3. 权限校验，有实体但是非题集创建者，也返回找不到，避免信息泄露
        if (!userId.equals(entity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND);
        }

        // 4. 更新题集数据
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setName(req.name());
        entity.setDescription(req.description());

        // 5. 回填数据库
        questionCollectionRepository.updateByModel(entity);

        // 6. 删除缓存
        String key = CacheKeyUtil.questionCollectionKey(userId);
        cacheService.delete(key);

        return CollectionVO.fromEntity(entity);
    }

    /**
     * 删除题集
     *
     * @param collectionId 题集ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCollection(Long collectionId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 获取待删除的题集实体
        QuestionCollectionEntity entity =  questionCollectionRepository.findById(collectionId).orElseThrow(
                () -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND));

        // 3. 权限校验，有实体但是非题集创建者，也返回找不到，避免信息泄露
        if (!userId.equals(entity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND);
        }

        // 4. 删除题集操作（内部实现了关联内容删除）
        questionCollectionRepository.deleteById(collectionId);

        // 5. 删除缓存
        String key = CacheKeyUtil.questionCollectionKey(userId);
        cacheService.delete(key);
    }

    /**
     * 获取当前登录用户的题集
     *
     * @return 题集列表视图
     */
    @Override
    public List<CollectionVO> getCollections(Long userId) {
        // 1. 查询缓存
        String key = CacheKeyUtil.questionCollectionKey(userId);
        List<CollectionVO> cache = cacheService.get(key);
        if (cache != null) {
            // 返回缓存
            return cache;
        }

        // 2. 获取题集列表并设置缓存
        List<CollectionVO> result = questionCollectionRepository.getCollectionsByUserId(userId);
        cacheService.set(key, result, CACHE_TTL_HOURS, TimeUnit.HOURS);
        return result;
    }

    private void ensureNameAvailable(Long ownerId, String name) {
        if (collectionPresenceCheckerPort.existsByOwnerIdAndName(ownerId, name)) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NAME_CONFLICT, name);
        }
    }

    private void ensureNamesAvailable(Long ownerId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return;
        }
        Set<String> seen = new HashSet<>();
        for (String name : names) {
            if (!seen.add(name)) {
                throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NAME_CONFLICT, name);
            }
        }
        List<String> exists = collectionPresenceCheckerPort.findExistingNames(ownerId, names);
        if (!exists.isEmpty()) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NAME_CONFLICT, exists.getFirst());
        }
    }
}
