package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.common.enums.SortDirectionEnum;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.adapter.utils.RepositoryUtils;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.CollectionItemMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionMapper;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
public class QuestionQueryRepositoryImpl implements QuestionQueryRepository {
    private final CollectionItemMapper collectionItemMapper;
    private final QuestionMapper questionMapper;

    public QuestionQueryRepositoryImpl(CollectionItemMapper collectionItemMapper, QuestionMapper questionMapper) {
        this.collectionItemMapper = collectionItemMapper;
        this.questionMapper = questionMapper;
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

    /**
     * 根据题目ID查询题目详情
     *
     * @param questionId 题目ID
     * @return 查询结果
     */
    @Override
    public Optional<QuestionDetailVO> getQuestionDetailById(Long questionId) {
        QueryChain<QuestionEntity> queryChain = QueryChain.of(questionMapper)
                .select(QuestionDetailVO.class)
                .join(QuestionEntity::getCurrentVersionId, QuestionVersionEntity::getId)
                .join(QuestionEntity::getCurrentVersionId, QuestionStat::getVersionId)
                .eq(QuestionEntity::getStatus, QuestionStatusEnum.ACTIVE.getCode())
                .eq(QuestionEntity::getId, questionId)
                .limit(1);
        return Optional.ofNullable(queryChain.returnType(QuestionDetailVO.class).get());
    }

    /**
     * 根据题集ID分页查询题集内题目概要
     *
     * @param collectionId 题集ID
     * @param query        分页查询参数
     * @return 分页结果
     */
    @Override
    public Pager<QuestionSummaryVO> findPageByCollectionId(Long collectionId,
                                                           QuestionInCollectionPageQuery query) {

        QueryChain<QuestionEntity> queryChain = QueryChain.of(questionMapper)
                .select(QuestionSummaryVO.class)
                .join(QuestionEntity::getId, CollectionItem::getQuestionId)
                .join(QuestionEntity::getCurrentVersionId, QuestionVersionEntity::getId)
                .join(QuestionEntity::getCurrentVersionId, QuestionStat::getVersionId)
                .eq(CollectionItem::getCollectionId, collectionId)
                .eq(QuestionEntity::getStatus, QuestionStatusEnum.ACTIVE.getCode())
                .ignoreNullValueInCondition(true)
                .ignoreEmptyInCondition(true)
                .trimStringInCondition(true)
                // 分页查询参数
                .eq(QuestionVersionEntity::getTypeCode, query.getTypeCode())
                .like(QuestionVersionEntity::getTitle, query.getKeyword());
        // 难度筛选
        if (query.getLevelMin() != null) {
            queryChain.gt(QuestionStat::getDifficulty, query.getLevelMin());
        }

        if (query.getLevelMax() != null) {
            queryChain.lt(QuestionStat::getDifficulty, query.getLevelMax());
        }

        // 排序处理
        if (StrUtil.isNotBlank(query.getSortField())) {
            switch (query.getSortField()) {
                case "createdAt" ->
                        RepositoryUtils.setSortDirectionCondition(queryChain, QuestionEntity::getCreatedAt, query.getSortDirection());
                case "typeCode" -> {
                    if (query.getSortDirection() == SortDirectionEnum.ASC) {
                        queryChain.orderBy(QuestionVersionEntity::getTypeCode);
                    } else {
                        queryChain.orderByDesc(QuestionVersionEntity::getTypeCode);
                    }
                }
                case "correctRate" -> {
                    if (query.getSortDirection() == SortDirectionEnum.ASC) {
                        queryChain.orderBy(QuestionStat::getCorrectRate);
                    } else {
                        queryChain.orderByDesc(QuestionStat::getCorrectRate);
                    }
                }
                default -> queryChain.orderBy(QuestionEntity::getCreatedAt);
            }
        } else {
            // 默认按题目创建日期升序排序
            queryChain.orderBy(QuestionEntity::getCreatedAt);
        }
        return queryChain.returnType(QuestionSummaryVO.class).paging(query.buildPager());
    }

    /**
     *
     * 根据题集IDs和题目类型查询题目ID列表
     * @param collectionIds 题集IDs
     * @param typeCode 题目类型
     * @return 题目ID列表
     */
    @Override
    public List<QuestionSummaryVO> findIdsByCollectionsAndType(List<Long> collectionIds, String typeCode) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        return QueryChain.of(questionMapper)
                .select(QuestionSummaryVO.class)
                .from(QuestionEntity.class)
                .join(QuestionEntity::getId, CollectionItem::getQuestionId)
                .join(QuestionEntity::getCurrentVersionId, QuestionVersionEntity::getId)
                .leftJoin(QuestionEntity::getCurrentVersionId, QuestionStat::getVersionId)
                // 条件筛选
                .in(CollectionItem::getCollectionId, collectionIds)
                .eq(QuestionVersionEntity::getTypeCode, typeCode)
                .eq(QuestionEntity::getStatus, QuestionStatusEnum.ACTIVE.getCode())
                .groupBy(QuestionEntity::getId)
                // 暂定按照难度升序排序
                .orderBy(QuestionStat::getDifficulty)
                .returnType(QuestionSummaryVO.class)
                .list();
    }
}
