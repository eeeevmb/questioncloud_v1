package cn.sztu.questioncloud.infrastructure.adapter.Paper;

import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PaperRepositoryImpl implements PaperRepository {

    private final PaperMapper paperMapper;

    /**
     * 根据ID查找试卷实体
     *
     * @param paperId 试卷ID
     * @return 试卷实体
     */
    @Override
    public PaperEntity getById(Long paperId) {
        return QueryChain.of(paperMapper)
                .eq(PaperEntity::getId, paperId)
                .limit(1)
                .get();
    }

    /**
     * 更新试卷实体
     *
     * @param paperEntity 试卷实体
     */
    @Override
    public void update(PaperEntity paperEntity) {
        paperMapper.update(paperEntity);
    }

    /**
     * 根据ID删除题目
     *
     * @param paperId 题目ID
     */
    @Override
    public void delete(Long paperId) {
        DeleteChain.of(paperMapper)
                .eq(PaperEntity::getId ,paperId)
                .execute();
    }

    /**
     * 保存试卷实体
     *
     * @param entity 试卷实体
     */
    @Override
    public void save(PaperEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        paperMapper.save(entity);
    }
}
