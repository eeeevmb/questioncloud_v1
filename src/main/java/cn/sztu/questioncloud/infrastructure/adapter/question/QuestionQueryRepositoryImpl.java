package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.CollectionItemMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionQueryRepositoryImpl implements QuestionQueryRepository {
    private final CollectionItemMapper collectionItemMapper;

    public QuestionQueryRepositoryImpl(CollectionItemMapper collectionItemMapper) {
        this.collectionItemMapper = collectionItemMapper;
    }

    /**
     * 查询题集内题目总数
     *
     * @param collectionId 题集ID
     * @return 题集内题目总数
     */
    @Override
    public Integer CountCollectionItemsById(Long collectionId) {
        return QueryChain.of(collectionItemMapper)
                .eq(CollectionItem::getCollectionId, collectionId)
                .count();
    }

    /**
     * 查询题集内最大顺序值
     *
     * @param collectionId 题集ID
     * @return 顺序值
     */
    @Override
    public Integer getMaxOrdinal(Long collectionId) {
        Integer result = QueryChain.of(collectionItemMapper)
                .select(CollectionItem::getOrdinal, c -> c.max())
                .eq(CollectionItem::getCollectionId, collectionId)
                .returnType(Integer.class)
                .get();
        return result == null ? 0 : result;
    }
}
