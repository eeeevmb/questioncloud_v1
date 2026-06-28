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
import java.util.Map;

@Repository
public class CollectionItemRepositoryImpl implements CollectionItemRepository {
    private final CollectionItemMapper collectionItemMapper;
    private final SqlSessionFactory sqlSessionFactory;

    public CollectionItemRepositoryImpl(CollectionItemMapper collectionItemMapper, SqlSessionFactory sqlSessionFactory) {
        this.collectionItemMapper = collectionItemMapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }


    /**
     * 根据题目ID和版本ID查询题集内容实体
     *
     * @param questionId 题目ID
     * @param versionId  版本ID
     * @return 题集内容实体
     */
    @Override
    public CollectionItem findByQuestionIdAndVersionId(Long questionId, Long versionId) {
        return QueryChain.of(collectionItemMapper)
                .eq(CollectionItem::getQuestionId, questionId)
                .eq(CollectionItem::getQuestionVersionId, versionId)
                .get();
    }

    /**
     * 根据题集ID查询题目版本ID列表
     *
     * @param collectionId 题集ID
     * @return 题目版本ID列表
     */
    @Override
    public List<Long> listVersionIdsByCollectionId(Long collectionId) {
        return QueryChain.of(collectionItemMapper)
                .select(CollectionItem::getQuestionVersionId)
                .eq(CollectionItem::getCollectionId, collectionId)
                .returnType(Long.class)
                .list();
    }

    @Override
    public List<Long> listVersionIdsByCollectionId(Long collectionId, Integer limit) {
        return QueryChain.of(collectionItemMapper)
                .select(CollectionItem::getQuestionVersionId)
                .eq(CollectionItem::getCollectionId, collectionId)
                .limit(limit)
                .returnType(Long.class)
                .list();
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
