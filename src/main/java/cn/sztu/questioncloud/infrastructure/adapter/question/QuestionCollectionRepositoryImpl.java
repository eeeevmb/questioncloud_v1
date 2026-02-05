package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.CollectionItemMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionCollectionMapper;
import cn.sztu.questioncloud.web.rest.v1.question.vo.CollectionVO;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class QuestionCollectionRepositoryImpl implements QuestionCollectionRepository {
    private final QuestionCollectionMapper questionCollectionMapper;
    private final CollectionItemMapper collectionItemMapper;

    public QuestionCollectionRepositoryImpl(QuestionCollectionMapper questionCollectionMapper, CollectionItemMapper collectionItemMapper) {
        this.questionCollectionMapper = questionCollectionMapper;
        this.collectionItemMapper = collectionItemMapper;
    }

    /**
     * 根据id查询题集
     *
     * @param collectionId 题集ID
     */
    @Override
    public Optional<QuestionCollectionEntity> findById(Long collectionId) {
        return Optional.ofNullable(QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getId, collectionId)
                .get());
    }

    /**
     * 根据用户ID获取题集列表
     *
     * @param userId 用户ID
     * @return 题集列表视图
     */
    @Override
    public List<CollectionVO> getCollectionsByUserId(Long userId) {
        return QueryChain.of(questionCollectionMapper)
                .eq(QuestionCollectionEntity::getOwnerId, userId)
                .returnType(CollectionVO.class)
                .list();
    }

    /**
     * 保存题集
     *
     * @param questionCollection 保存的题集对象
     */
    @Override
    public void save(QuestionCollectionEntity questionCollection) {
        if (questionCollection.getId() == null) {
            questionCollection.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        questionCollectionMapper.save(questionCollection);
    }

    /**
     * 通过实体更新题集
     *
     * @param questionCollection 题集实体
     */
    @Override
    public void updateByModel(QuestionCollectionEntity questionCollection) {
        questionCollectionMapper.update(questionCollection);
    }

    /**
     * 根据ID删除题集，以及题集内容
     *
     * @param collectionId 题集ID
     * @return 是否成功
     */
    @Override
    @Transactional
    public boolean deleteById(Long collectionId) {
        // 先删除所有题集内容，然后删除题集本身
        DeleteChain.of(collectionItemMapper)
                .eq(CollectionItem::getCollectionId, collectionId)
                .execute();

        return questionCollectionMapper.deleteById(collectionId) > 0;
    }
}
