package cn.sztu.questioncloud.infrastructure.adapter.paper;

import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.sztu.questioncloud.common.enums.SortDirectionEnum;
import cn.sztu.questioncloud.infrastructure.adapter.utils.RepositoryUtils;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperItemMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperMapper;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaperQueryRepositoryImpl implements PaperQueryRepository {

    private final PaperMapper paperMapper;
    private final PaperItemMapper paperItemMapper;

    /**
     * 根据试卷ID查询题目详情
     */
    @Override
    public PaperDetailVO getBasicPaperById(Long paperId) {
        return QueryChain.of(paperMapper)
                .select(PaperDetailVO.class)
                .from(PaperEntity.class)
                .eq(PaperEntity::getId, paperId)
                .returnType(PaperDetailVO.class)
                .get();
    }

    /**
     * 分页查询试卷列表
     */
    @Override
    public Pager<PaperBasicVO> searchPapers(PaperPageQuery query, Long userId) {
        QueryChain<PaperEntity> queryChain = QueryChain.of(paperMapper)
                .select(PaperBasicVO.class)
                .from(PaperEntity.class)
                .eq(PaperEntity::getOwnerId, userId)
                .ignoreNullValueInCondition(true)
                .ignoreEmptyInCondition(true)
                .trimStringInCondition(true)
                .like(PaperEntity::getTitle, query.getKeyword());

        // 排序处理
        if (StrUtil.isNotBlank(query.getSortField())) {
            switch (query.getSortField()) {
                case "createdAt" ->
                        RepositoryUtils.setSortDirectionCondition(queryChain, PaperEntity::getCreatedAt, query.getSortDirection());
                case "updatedAt" ->
                        RepositoryUtils.setSortDirectionCondition(queryChain, PaperEntity::getUpdatedAt, query.getSortDirection());
                default -> queryChain.orderBy(QuestionEntity::getCreatedAt);
            }
        } else {
            // 默认按试卷创建日期升序排序
            queryChain.orderBy(QuestionEntity::getCreatedAt);
        }
        return queryChain.returnType(PaperBasicVO.class).paging(query.buildPager());
    }

    /**
     * 查询试卷关联的题目详情列表
     */
    @Override
    public List<PaperItemDetailVO> listPaperItemsByPaperId(Long paperId) {
        return QueryChain.of(paperItemMapper)
                .select(PaperItemDetailVO.class)
                .from(PaperItemEntity.class)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionVersionEntity::getId)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionStat::getVersionId)
                .eq(PaperItemEntity::getPaperId, paperId)
                .orderBy(PaperItemEntity::getSeq)
                .returnType(PaperItemDetailVO.class)
                .list();
    }

    /**
     * 根据试卷ID查询题目总数
     */
    @Override
    public Integer countItemsByPaperId(Long paperId){
        return QueryChain.of(paperMapper)
                .select(PaperEntity::getTotalItems)
                .eq(PaperItemEntity::getPaperId, paperId)
                .returnType(Integer.class)
                .get();
    }

    /**
     * 根据试卷ID查询试卷总分
     */
    @Override
    public BigDecimal sumScoreByPaperId(Long paperId){
        return QueryChain.of(paperMapper)
                .select(PaperEntity::getTotalScore)
                .eq(PaperEntity::getId, paperId)
                .returnType(BigDecimal.class)
                .get();
    }
}
