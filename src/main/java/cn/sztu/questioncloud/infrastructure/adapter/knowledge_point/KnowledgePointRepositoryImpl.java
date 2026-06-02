package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point;

import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.knowledge_point.KnowledgePointMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import cn.xbatis.core.sql.executor.chain.UpdateChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class KnowledgePointRepositoryImpl implements KnowledgePointRepository{

    private final KnowledgePointMapper knowledgePointMapper;

    @Override
    public KnowledgePointEntity getById(Long id) {
        return QueryChain.of(knowledgePointMapper)
                .eq(KnowledgePointEntity::getId, id)
                .eq(KnowledgePointEntity::getIsDeleted, 0)
                .limit(1)
                .get();
    }

    @Override
    public KnowledgePointEntity searchCanonicalNameOrAlias(String subject, String name) {
        return QueryChain.of(knowledgePointMapper)
                .eq(KnowledgePointEntity::getSubject, subject)
                .eq(KnowledgePointEntity::getIsDeleted, 0)
                .andNested(g -> g
                        .eq(KnowledgePointEntity::getCanonicalName, name)
                        .or().like(KnowledgePointEntity::getAliases, name))
                .limit(1)
                .get();
    }

    @Override
    public List<KnowledgePointEntity> listByCanonicalNameOrAlias(String subject, String name) {
        return QueryChain.of(knowledgePointMapper)
                .eq(KnowledgePointEntity::getSubject, subject)
                .eq(KnowledgePointEntity::getIsDeleted, 0)
                .andNested(g -> g
                        .eq(KnowledgePointEntity::getCanonicalName, name)
                        .or().like(KnowledgePointEntity::getAliases, name))
                .list();
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
                .set(KnowledgePointEntity::getSubject, entity.getSubject())
                .set(KnowledgePointEntity::getCanonicalName, entity.getCanonicalName())
                .set(KnowledgePointEntity::getDescription, entity.getDescription())
                .set(KnowledgePointEntity::getAliases, entity.getAliases())
                .set(KnowledgePointEntity::getFormulaOrCode, entity.getFormulaOrCode())
                .set(KnowledgePointEntity::getExample, entity.getExample())
                .set(KnowledgePointEntity::getUpdatedAt, LocalDateTime.now())
                .eq(KnowledgePointEntity::getId, entity.getId())
                .execute();
    }


}
