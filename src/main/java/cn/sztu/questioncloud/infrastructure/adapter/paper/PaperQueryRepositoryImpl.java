package cn.sztu.questioncloud.infrastructure.adapter.paper;

import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.hutool.core.util.StrUtil;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperItemMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperMapper;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperListItemVO;
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
     *
     * @param paperId 试卷ID
     * @return 查询结果
     */
    @Override
    public Optional<PaperDetailVO> getPaperDetailById(Long paperId){
        // 1. 查询试卷主表信息
        PaperDetailVO paperVO = QueryChain.of(paperMapper)
                .select(PaperDetailVO.class)
                .eq(PaperEntity::getId, paperId)
                .returnType(PaperDetailVO.class)
                .limit(1)
                .get();
        // 如果试卷不存在返回空，不再查题目
        if (paperVO == null) {
            return Optional.empty();
        }

        // 2. 查询关联的题目列表 (Items List)
        List<PaperItemVO> itemList = QueryChain.of(paperItemMapper)
                .select(PaperItemVO.class)
                // 关联 Question 表获取标题 (对应 PaperItemVO 里的 @ResultEntityField)
                //.leftJoin(PaperItemEntity::getQuestionId, QuestionEntity::getId)
                // 关联 Version 表获取题干 (对应 PaperItemVO 里的 stem)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionVersionEntity::getId)
                // 关联 Stat 表获取难度 (对应 PaperItemVO 里的 difficulty)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionStat::getVersionId)
                .eq(PaperItemEntity::getPaperId, paperId)
                .orderBy(PaperItemEntity::getSeq) // 按照题号排序
                .returnType(PaperItemVO.class)
                .list();

        // 3. 组装返回视图
        paperVO.setItems(itemList);
        return Optional.of(paperVO);
    }

    /**
     * 根据试卷ID查询题目总数
     *
     * @param paperId 试卷ID
     * @return 题目总数
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
     *
     * @param paperId 试卷ID
     * @return 试卷总分
     */
    @Override
    public BigDecimal sumScoreByPaperId(Long paperId){
        return QueryChain.of(paperMapper)
                .select(PaperEntity::getTotalScore)
                .eq(PaperEntity::getId, paperId)
                .returnType(BigDecimal.class)
                .get();
    }

    @Override
    public Pager<PaperListItemVO> pageByOwnerId(Long ownerId, PaperPageQuery query) {
        var queryChain = QueryChain.of(paperMapper)
                .select(PaperListItemVO.class)
                .trimStringInCondition(true)
                .eq(PaperEntity::getOwnerId, ownerId)
                .orderByDesc(PaperEntity::getUpdatedAt);

        if (query.getStatus() != null) {
            queryChain.eq(PaperEntity::getStatus, query.getStatus());
        }
        if (StrUtil.isNotBlank(query.getKeyword())) {
            queryChain.like(PaperEntity::getTitle, query.getKeyword());
        }

        return queryChain.returnType(PaperListItemVO.class).paging(query.buildPager());
    }
}
