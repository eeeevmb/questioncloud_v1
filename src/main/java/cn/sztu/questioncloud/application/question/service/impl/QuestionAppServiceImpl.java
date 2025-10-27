package cn.sztu.questioncloud.application.question.service.impl;

import cn.sztu.questioncloud.application.question.enums.CollectionSourceEnum;
import cn.sztu.questioncloud.application.question.port.CollectionPresenceCheckerPort;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 题目模块应用服务实现
 *
 * @author eeeevmb
 */
@Slf4j
@Service
public class QuestionAppServiceImpl implements QuestionAppService {
    private final CollectionPresenceCheckerPort collectionPresenceCheckerPort;
    private final QuestionCollectionRepository questionCollectionRepository;

    public QuestionAppServiceImpl(CollectionPresenceCheckerPort collectionPresenceCheckerPort, QuestionCollectionRepository questionCollectionRepository) {
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
}
