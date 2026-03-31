package cn.sztu.questioncloud.infrastructure.adapter.question;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.question.enums.QuestionStatusEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.common.enums.SortDirectionEnum;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.adapter.utils.RepositoryUtils;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.*;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.CollectionItemMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionCollectionMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.question.QuestionMapper;
import cn.sztu.questioncloud.web.rest.v1.question.req.QuestionInCollectionPageQuery;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class QuestionQueryRepositoryImpl implements QuestionQueryRepository {
    private final CollectionItemMapper collectionItemMapper;
    private final QuestionMapper questionMapper;
    private final QuestionCollectionMapper questionCollectionMapper;

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
     * 查询题目ID-题集实体映射表
     *
     * @return 映射表
     */
    @Override
    public Map<Long, QuestionCollectionEntity> getQuestionIdToCollectionMap(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<CollectionItem> relationList = QueryChain.of(collectionItemMapper)
                .select(CollectionItem::getQuestionId, CollectionItem::getCollectionId)
                .in(CollectionItem::getQuestionId, questionIds)
                // 保证同一题在多个题集时映射结果稳定（取最小 collection_id）
                .orderBy(CollectionItem::getCollectionId)
                .list();
        if (relationList == null || relationList.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> collectionIds = relationList.stream()
                .map(CollectionItem::getCollectionId)
                .distinct()
                .toList();

        Map<Long, QuestionCollectionEntity> collectionById = QueryChain.of(questionCollectionMapper)
                .in(QuestionCollectionEntity::getId, collectionIds)
                .list()
                .stream()
                .collect(Collectors.toMap(
                        QuestionCollectionEntity::getId,
                        Function.identity(),
                        (left, right) -> left
                ));

        Map<Long, QuestionCollectionEntity> result = new LinkedHashMap<>();
        for (CollectionItem relation : relationList) {
            QuestionCollectionEntity collection = collectionById.get(relation.getCollectionId());
            if (collection != null) {
                result.putIfAbsent(relation.getQuestionId(), collection);
            }
        }
        return result;
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
            queryChain.gte(QuestionStat::getDifficulty, query.getLevelMin());
        }

        if (query.getLevelMax() != null) {
            queryChain.lte(QuestionStat::getDifficulty, query.getLevelMax());
        }

        // 排序处理
        if (StrUtil.isNotBlank(query.getSortField())) {
            switch (query.getSortField()) {
                case "createdAt" ->
                        RepositoryUtils.setSortDirectionCondition(queryChain, QuestionEntity::getCreatedAt, query.getSortDirection());
                case "updatedAt" ->
                        RepositoryUtils.setSortDirectionCondition(queryChain, QuestionEntity::getUpdatedAt, query.getSortDirection());
                case "difficulty" -> {
                    if (query.getSortDirection() == SortDirectionEnum.ASC) {
                        queryChain.orderBy(QuestionStat::getDifficulty);
                    } else {
                        queryChain.orderByDesc(QuestionStat::getDifficulty);
                    }
                }
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
    public List<QuestionDetailVO> findIdsByCollectionsAndType(List<Long> collectionIds, String typeCode) {
        if (collectionIds == null || collectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 正常执行连表查询（不加 groupBy）
        // 此时查出来的数据已经按难度排好了序，但是会有重复记录
        List<QuestionDetailVO> rawList = QueryChain.of(questionMapper)
                .select(QuestionDetailVO.class) // 💡 提醒：既然返回 DetailVO，这里最好也 Select 它，否则会丢字段
                .from(QuestionEntity.class)
                .join(QuestionEntity::getId, CollectionItem::getQuestionId)
                .join(QuestionEntity::getCurrentVersionId, QuestionVersionEntity::getId)
                .leftJoin(QuestionEntity::getCurrentVersionId, QuestionStat::getVersionId)
                .in(CollectionItem::getCollectionId, collectionIds)
                .eq(QuestionVersionEntity::getTypeCode, typeCode)
                .eq(QuestionEntity::getStatus, QuestionStatusEnum.ACTIVE.getCode())
                .orderBy(QuestionStat::getDifficulty) // 依然交由数据库排序
                .returnType(QuestionDetailVO.class)
                .list();

        // 2. 在 Java 内存中去重，并保留 SQL 的排序结果
        Map<Long, QuestionDetailVO> distinctMap = new LinkedHashMap<>();
        for (QuestionDetailVO vo : rawList) {
            // 如果 map 里还没存过这个题目 ID，就放进去。后续查出来的重复题目会直接被忽略。
            distinctMap.putIfAbsent(vo.getId(), vo);
        }

        // 3. 返回去重后的纯净列表
        return new ArrayList<>(distinctMap.values());
    }
}