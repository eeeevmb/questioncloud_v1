package cn.sztu.questioncloud.application.importer.service;

import cn.sztu.questioncloud.application.question.enums.CollectionSourceEnum;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionCollectionMapper;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 题集自动创建器。
 *
 * @author Codex
 */
@Component
public class CollectionAutoCreator {
    private final QuestionCollectionRepository questionCollectionRepository;
    private final QuestionCollectionMapper questionCollectionMapper;

    public CollectionAutoCreator(QuestionCollectionRepository questionCollectionRepository,
                                 QuestionCollectionMapper questionCollectionMapper) {
        this.questionCollectionRepository = questionCollectionRepository;
        this.questionCollectionMapper = questionCollectionMapper;
    }

    public QuestionCollectionEntity ensureCollection(Long ownerId, String name) {
        QuestionCollectionEntity existing = QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getOwnerId, ownerId)
                .eq(QuestionCollectionEntity::getName, name)
                .get();
        if (existing != null) {
            return existing;
        }
        QuestionCollectionEntity entity = QuestionCollectionEntity.builder()
                .id(HutoolSnowflakeIdGenerator.generateLongId())
                .name(name)
                .ownerId(ownerId)
                .source(CollectionSourceEnum.USER.getCode())
                .build();
        try {
            questionCollectionRepository.save(entity);
            return entity;
        } catch (DuplicateKeyException ex) {
            return QueryChain.of(questionCollectionMapper)
                    .eq(QuestionCollectionEntity::getOwnerId, ownerId)
                    .eq(QuestionCollectionEntity::getName, name)
                    .get();
        }
    }
}
