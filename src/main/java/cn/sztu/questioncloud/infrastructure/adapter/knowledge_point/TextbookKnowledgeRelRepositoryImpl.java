package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.TextbookKnowledgeRelRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.TextbookKnowledgeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgePointMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.TextbookKnowledgeRelMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TextbookKnowledgeRelRepositoryImpl implements TextbookKnowledgeRelRepository {
    private final TextbookKnowledgeRelMapper textbookKnowledgeRelMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    @Override
    public List<TextbookKnowledgeRelEntity> listByTextbookId(Long textbookId) {
        return QueryChain.of(textbookKnowledgeRelMapper)
                .eq(TextbookKnowledgeRelEntity::getTextbookId, textbookId)
                .returnType(TextbookKnowledgeRelEntity.class)
                .list();
    }

    @Override
    public List<KnowledgePointEntity> getByTextbookId(Long textbookId) {
        List<Long> knowledgePointIds = QueryChain.of(textbookKnowledgeRelMapper)
                .select(TextbookKnowledgeRelEntity::getKnowledgePointId)
                .eq(TextbookKnowledgeRelEntity::getTextbookId, textbookId)
                .returnType(Long.class)
                .list();
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return List.of();
        }
        return QueryChain.of(knowledgePointMapper)
                .in(KnowledgePointEntity::getId, knowledgePointIds)
                .returnType(KnowledgePointEntity.class)
                .list();
    }

    @Override
    public void save(TextbookKnowledgeRelEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        textbookKnowledgeRelMapper.save(entity);
    }

    @Override
    public void deleteByTextbookId(Long textbookId) {
        DeleteChain.of(textbookKnowledgeRelMapper)
                .eq(TextbookKnowledgeRelEntity::getTextbookId, textbookId)
                .execute();
    }

    @Override
    public boolean existsByKnowledgePointId(Long knowledgePointId) {
        return QueryChain.of(textbookKnowledgeRelMapper)
                .eq(TextbookKnowledgeRelEntity::getKnowledgePointId, knowledgePointId)
                .exists();
    }

    @Override
    public void delete(Long id) {
        DeleteChain.of(textbookKnowledgeRelMapper)
                .eq(TextbookKnowledgeRelEntity::getId, id)
                .execute();
    }
}
