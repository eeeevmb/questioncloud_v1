package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.CollectionPresenceCheckerPort;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionCollectionMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CollectionPresenceCheckerAdapter implements CollectionPresenceCheckerPort {
    private final QuestionCollectionMapper questionCollectionMapper;

    public CollectionPresenceCheckerAdapter(QuestionCollectionMapper questionCollectionMapper) {
        this.questionCollectionMapper = questionCollectionMapper;
    }

    /**
     * 检查默认题集是否已存在
     *
     * @param userId 用户ID
     * @return 存在返回true
     */
    @Override
    public boolean existsDefaultByUserId(Long userId) {
        return QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getOwnerId, userId)
                .eq(QuestionCollectionEntity::getName, "默认题集")
                .eq(QuestionCollectionEntity::getSource, 1)
                .exists();
    }

    /**
     * 检查题集是否已存在
     *
     * @param collectionId 题集ID
     * @return 存在返回true
     */
    @Override
    public boolean existsById(Long collectionId) {
        return QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getId, collectionId)
                .exists();
    }

    @Override
    public boolean existsByOwnerIdAndName(Long ownerId, String name) {
        return QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getOwnerId, ownerId)
                .eq(QuestionCollectionEntity::getName, name)
                .exists();
    }

    @Override
    public List<String> findExistingNames(Long ownerId, List<String> names) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }
        return QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getOwnerId, ownerId)
                .in(QuestionCollectionEntity::getName, names)
                .select(QuestionCollectionEntity::getName)
                .list()
                .stream()
                .map(QuestionCollectionEntity::getName)
                .toList();
    }
}
