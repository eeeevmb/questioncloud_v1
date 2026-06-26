package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgeQuestionRelMapper;
import cn.xbatis.core.mybatis.MybatisBatchUtil;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeQuestionRelRepositoryImpl implements KnowledgeQuestionRelRepository{

    private final KnowledgeQuestionRelMapper knowledgeQuestionRelMapper;
    private final SqlSessionFactory sqlSessionFactory;

    @Override
    public List<KnowledgeQuestionRelEntity> listByQuestionVersionId(Long questionVersionId) {
        return QueryChain.of(knowledgeQuestionRelMapper)
                .eq(KnowledgeQuestionRelEntity::getQuestionVersionId, questionVersionId)
                .returnType(KnowledgeQuestionRelEntity.class)
                .list();
    }

    @Override
    public void deleteRelationsByQuestionVersionId(Long questionVersionId) {
        DeleteChain.of(knowledgeQuestionRelMapper)
                .eq(KnowledgeQuestionRelEntity::getQuestionVersionId, questionVersionId)
                .execute();
    }

    @Override
    public void save(KnowledgeQuestionRelEntity entity) {
        if(entity.getId() == null){
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        knowledgeQuestionRelMapper.save(entity);
    }

    @Override
    public void batchSave(List<KnowledgeQuestionRelEntity> entities) {
        if (entities == null || entities.isEmpty())
            return;
        for (KnowledgeQuestionRelEntity entity : entities) {
            if (entity.getId() == null)
                entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        MybatisBatchUtil.batchSave(sqlSessionFactory, KnowledgeQuestionRelMapper.class, entities);
    }

    @Override
    public void deleteByKnowledgePointId(Long knowledgePointId) {
        DeleteChain.of(knowledgeQuestionRelMapper)
                .eq(KnowledgeQuestionRelEntity::getKnowledgePointId, knowledgePointId)
                .execute();
    }

    @Override
    public void deleteByQuestionId(Long questionId) {
        DeleteChain.of(knowledgeQuestionRelMapper)
                .eq(KnowledgeQuestionRelEntity::getQuestionId, questionId)
                .execute();
    }

    @Override
    public boolean existsByKnowledgePointId(Long knowledgePointId) {
        return QueryChain.of(knowledgeQuestionRelMapper)
                .eq(KnowledgeQuestionRelEntity::getKnowledgePointId, knowledgePointId)
                .exists();
    }
}
