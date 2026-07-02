package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointScopeRelRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgePointMapper;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgePointScopeRelMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgePointScopeRelRepositoryImpl implements KnowledgePointScopeRelRepository {

    private final KnowledgePointScopeRelMapper knowledgePointScopeRelMapper;
    private final KnowledgePointMapper knowledgePointMapper;

    @Override
    public KnowledgePointScopeRelEntity getById(Long id) {
        return QueryChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getId, id)
                .get();
    }

    @Override
    public List<KnowledgePointScopeRelEntity> listByKnowledgePointId(Long knowledgePointId) {
        return QueryChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getKnowledgePointId, knowledgePointId)
                .returnType(KnowledgePointScopeRelEntity.class)
                .list();
    }

    @Override
    public boolean exists(Long knowledgePointId, Long knowledgeScopeId) {
        return QueryChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getKnowledgePointId, knowledgePointId)
                .eq(KnowledgePointScopeRelEntity::getKnowledgeScopeId, knowledgeScopeId)
                .exists();
    }

    /**
     * 根据知识范围ID查询知识点ID列表
     *
     * @param knowledgeScopeIds 知识范围ID
     * @return 知识点 name列表
     */
    @Override
    public List<String> listKnowledgePointNamesByKnowledgeScopeIds(List<Long> knowledgeScopeIds) {
        if (knowledgeScopeIds == null || knowledgeScopeIds.isEmpty()) {
            return List.of();
        }

        List<Long> knowledgePointIds = QueryChain.of(knowledgePointScopeRelMapper)
                .in(KnowledgePointScopeRelEntity::getKnowledgeScopeId, knowledgeScopeIds)
                .select(KnowledgePointScopeRelEntity::getKnowledgePointId)
                .returnType(Long.class)
                .list()
                .stream()
                .filter(id -> id != null)
                .distinct()
                .toList();

        if (knowledgePointIds.isEmpty()) {
            return List.of();
        }

        return QueryChain.of(knowledgePointMapper)
                .in(KnowledgePointEntity::getId, knowledgePointIds)
                .select(KnowledgePointEntity::getCanonicalName)
                .returnType(String.class)
                .list()
                .stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    //=== BASIC ===

    @Override
    public void save(KnowledgePointScopeRelEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        knowledgePointScopeRelMapper.save(entity);
    }

    @Override
    public void delete(Long id) {
        DeleteChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getId, id)
                .execute();
    }

    @Override
    public void deleteByKnowledgePointId(Long knowledgePointId) {
        DeleteChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getKnowledgePointId, knowledgePointId)
                .execute();
    }

    @Override
    public void deleteByKnowledgeScopeId(Long knowledgeScopeId) {
        DeleteChain.of(knowledgePointScopeRelMapper)
                .eq(KnowledgePointScopeRelEntity::getKnowledgeScopeId, knowledgeScopeId)
                .execute();
    }
}
