package cn.sztu.questioncloud.infrastructure.adapter.paper;

import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperItemMapper;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemDetailVO;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PaperItemRepositoryImpl implements PaperItemRepository {

    private final PaperItemMapper paperItemMapper;

    /**
     * 根据试卷ID删除试卷下所有试题关联
     *
     * @param paperId 试卷ID
     */
    @Override
    public void deleteItemsByPaperId(Long paperId){
        DeleteChain.of(paperItemMapper)
                .eq(PaperItemEntity::getPaperId, paperId)
                .execute();
    }

    /**
     * 根据试卷ID获取试卷下所有实体详情
     *
     * @param paperId 试卷ID
     * @return 试卷题目详情实体列表
     */
    @Override
    public List<PaperItemDetailVO> getDetailedItemsByPaperId(Long paperId){
        return QueryChain.of(paperItemMapper)
                .select(PaperItemDetailVO.class)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionVersionEntity::getId)
                .leftJoin(PaperItemEntity::getQuestionVersionId, QuestionStat::getVersionId)
                .eq(PaperItemEntity::getPaperId, paperId)
                .orderBy(PaperItemEntity::getSeq)
                .returnType(PaperItemDetailVO.class)
                .list();
    }

    /**
     * 根据试卷ID保存单个试题联系
     *
     * @param entity 试卷题目实体
     */
    @Override
    public void saveItem(PaperItemEntity entity){
        paperItemMapper.save(entity);
    }

    /**
     * 根据试卷ID保存多个试题联系(常用)
     *
     * @param entities 试卷题目实体列表
     */
    @Override
    public void saveBatchItems(List<PaperItemEntity> entities){
        if (entities == null || entities.isEmpty()) {
            return;
        }
        paperItemMapper.saveBatch(entities);
    }
}
