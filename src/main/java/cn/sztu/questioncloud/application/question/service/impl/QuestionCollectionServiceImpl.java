package cn.sztu.questioncloud.application.question.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.question.enums.CollectionSourceEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.port.CollectionPresenceCheckerPort;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.application.question.service.QuestionCollectionService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.req.UpdateCollectionReq;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 题目模块应用服务实现
 *
 * @author eeeevmb
 */
@Slf4j
@Service
public class QuestionCollectionServiceImpl implements QuestionCollectionService {
    private final CollectionPresenceCheckerPort collectionPresenceCheckerPort;
    private final QuestionCollectionRepository questionCollectionRepository;

    public QuestionCollectionServiceImpl(CollectionPresenceCheckerPort collectionPresenceCheckerPort, QuestionCollectionRepository questionCollectionRepository) {
        this.collectionPresenceCheckerPort = collectionPresenceCheckerPort;
        this.questionCollectionRepository = questionCollectionRepository;
    }

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
            log.info("存在默认题集，不进行创建操作。");
            return;
        }
        // 2. 创建默认题集
        log.info("开始创建默认题集");
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

        // 2. 构建题集实体
        QuestionCollectionEntity collection = QuestionCollectionEntity.builder()
                .name(req.name())
                .description(req.description())
                .ownerId(userId)
                .source(CollectionSourceEnum.USER.getCode())
                .build();
        questionCollectionRepository.save(collection);

        // 3. 返回视图对象
        return CollectionVO.fromEntity(collection);
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
                () -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在"));

        // 3. 权限校验，有实体但是非题集创建者，也返回找不到，避免信息泄露
        if (!userId.equals(entity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在");
        }

        // 4. 更新题集数据
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setName(req.name());
        entity.setDescription(req.description());

        // 5. 回填数据库
        questionCollectionRepository.updateByModel(entity);

        return CollectionVO.fromEntity(entity);
    }

    /**
     * 删除题集
     *
     * @param collectionId 题集ID
     */
    @Override
    public void deleteCollection(Long collectionId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 获取待删除的题集实体
        QuestionCollectionEntity entity =  questionCollectionRepository.findById(collectionId).orElseThrow(
                () -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在"));

        // 3. 权限校验，有实体但是非题集创建者，也返回找不到，避免信息泄露
        if (!userId.equals(entity.getOwnerId())) {
            throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在");
        }

        // 4. 删除题集操作（内部实现了关联内容删除）
        questionCollectionRepository.deleteById(collectionId);
    }
}
