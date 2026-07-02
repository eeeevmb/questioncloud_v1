package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgeScopeMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgeScopeRepositoryImpl implements KnowledgeScopeRepository {

    private final KnowledgeScopeMapper knowledgeScopeMapper;

    @Override
    public KnowledgeScopeEntity getById(Long id) {
        return QueryChain.of(knowledgeScopeMapper)
                .eq(KnowledgeScopeEntity::getId, id)
                .get();
    }

    @Override
    public KnowledgeScopeEntity getByScopeName(String scopeName) {
        return QueryChain.of(knowledgeScopeMapper)
                .eq(KnowledgeScopeEntity::getScopeName, scopeName)
                .eq(KnowledgeScopeEntity::getIsDeleted, 0)
                .get();
    }

    @Override
    public List<String> getAllScopeNames() {
        return QueryChain.of(knowledgeScopeMapper)
                .select(KnowledgeScopeEntity::getScopeName)
                .eq(KnowledgeScopeEntity::getIsDeleted, 0)
                .returnType(String.class)
                .list();
    }

    @Override
    public void save(KnowledgeScopeEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        knowledgeScopeMapper.save(entity);
    }

    @Override
    public void update(KnowledgeScopeEntity entity) {
        UpdateChain.of(knowledgeScopeMapper)
                .update(KnowledgeScopeEntity.class)
                .set(KnowledgeScopeEntity::getScopeName, entity.getScopeName())
                .set(KnowledgeScopeEntity::getDescription, entity.getDescription())
                .set(KnowledgeScopeEntity::getCreatedBy, entity.getCreatedBy())
                .set(KnowledgeScopeEntity::getUpdatedAt, entity.getUpdatedAt())
                .set(KnowledgeScopeEntity::getIsDeleted, entity.getIsDeleted())
                .eq(KnowledgeScopeEntity::getId, entity.getId())
                .execute();
    }

    @Override
    public void delete(Long id) {
        UpdateChain.of(knowledgeScopeMapper)
                .update(KnowledgeScopeEntity.class)
                .set(KnowledgeScopeEntity::getIsDeleted, 1)
                .eq(KnowledgeScopeEntity::getId, id)
                .execute();
    }
}
