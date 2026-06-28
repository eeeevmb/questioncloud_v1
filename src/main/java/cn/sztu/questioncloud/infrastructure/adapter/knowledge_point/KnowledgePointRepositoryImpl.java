package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgePointMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgePointRepositoryImpl implements KnowledgePointRepository {

    private final KnowledgePointMapper knowledgePointMapper;

    @Override
    public List<KnowledgePointEntity> listByCanonicalNameOrAlias(String name) {
        return QueryChain.of(knowledgePointMapper)
                .andNested(g -> g
                        .eq(KnowledgePointEntity::getCanonicalName, name)
                        .or().like(KnowledgePointEntity::getAliases, name))
                .list();
    }

    @Override
    public List<KnowledgePointEntity> listNeedEnrich(Integer limit) {
        return QueryChain.of(knowledgePointMapper)
                .andNested(g -> g
                        .isNull(KnowledgePointEntity::getDescription)
                        .or().eq(KnowledgePointEntity::getDescription, "")
                        .or().isNull(KnowledgePointEntity::getExample)
                        .or().eq(KnowledgePointEntity::getExample, "")
                        .or().isNull(KnowledgePointEntity::getFormulaOrCode)
                        .or().eq(KnowledgePointEntity::getFormulaOrCode, ""))
                .limit(limit)
                .list();
    }

    @Override
    public List<KnowledgePointEntity> listAllActive() {
        return QueryChain.of(knowledgePointMapper).list();
    }

    /// === BASIC ===

    @Override
    public KnowledgePointEntity getById(Long id) {
        return QueryChain.of(knowledgePointMapper)
                .eq(KnowledgePointEntity::getId, id)
                .limit(1)
                .get();
    }

    @Override
    public void save(KnowledgePointEntity entity) {
        if (entity.getId() == null) {
            entity.setId(HutoolSnowflakeIdGenerator.generateLongId());
        }
        knowledgePointMapper.save(entity);
    }

    @Override
    public void update(KnowledgePointEntity entity) {
        UpdateChain.of(knowledgePointMapper)
                .update(KnowledgePointEntity.class)
                .set(KnowledgePointEntity::getCanonicalName, entity.getCanonicalName())
                .set(KnowledgePointEntity::getDescription, entity.getDescription())
                .set(KnowledgePointEntity::getAliases, entity.getAliases())
                .set(KnowledgePointEntity::getFormulaOrCode, entity.getFormulaOrCode())
                .set(KnowledgePointEntity::getExample, entity.getExample())
                .set(KnowledgePointEntity::getUpdatedAt, LocalDateTime.now())
                .eq(KnowledgePointEntity::getId, entity.getId())
                .execute();
    }

    @Override
    public void delete(Long id) {
        DeleteChain.of(knowledgePointMapper)
                .eq(KnowledgePointEntity::getId, id)
                .execute();
    }
}
