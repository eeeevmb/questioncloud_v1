package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.CollectionItemRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.CollectionItemMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CollectionItemRepositoryImpl implements CollectionItemRepository {
    private final CollectionItemMapper collectionItemMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public CollectionItemRepositoryImpl(CollectionItemMapper collectionItemMapper, SqlSessionFactory sqlSessionFactory) {
        this.collectionItemMapper = collectionItemMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    /**
     * 根据题集ID和题目ID查询题集内容实体
     *
     * @param collectionId 题集ID
     * @param questionId   题目ID
     * @return 题集内容实体
     */
    @Override
    public CollectionItem findByQuestionIdAndCollectionId(Long collectionId, Long questionId) {
        return QueryChain.of(collectionItemMapper)
                .eq(CollectionItem::getCollectionId, collectionId)
                .eq(CollectionItem::getQuestionId, questionId)
                .limit(1)
                .get();
    }

    /**
     * 将试题存入题集
     *
     * @param collectionItem 题集内容
     */
    @Override
    public void save(CollectionItem collectionItem) {
        collectionItemMapper.save(collectionItem);
    }

    /**
     * 根据题目ID删除题集关联
     *
     * @param questionId 题目ID
     */
    @Override
    public void deleteByQuestionId(Long questionId) {
        DeleteChain.of(collectionItemMapper)
                .eq(CollectionItem::getQuestionId, questionId)
                .execute();
    }

    /**
     * 批量存入题集
     *
     * @param items 题集内容
     */
    @Override
    public void batchSave(List<CollectionItem> items) {
        MybatisBatchUtil.batchSave(sqlSessionFactory, CollectionItemMapper.class, items);
    }

    /**
     * 根据题目ID更新题集内容的版本ID
     * 用于更新题目后题集内容表同步
     *
     * @param questionId   题目ID
     * @param newVersionId 新题目版本ID
     */
    @Override
    public void syncVersionToAllCollections(Long questionId, Long newVersionId) {
        UpdateChain.of(collectionItemMapper)
                .set(CollectionItem::getQuestionVersionId, newVersionId)
                .eq(CollectionItem::getQuestionId, questionId)
                .execute();
    }
}
