package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.enums.AgentStatusEnum;
import cn.sztu.questioncloud.application.ai.port.AgentRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent.AgentEntityMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AgentRepositoryImpl implements AgentRepository {
    private final AgentEntityMapper agentEntityMapper;

    @Override
    public Map<Long, AgentEntity> getEntityMap() {
        return QueryChain.of(agentEntityMapper)
                .eq(AgentEntity::getStatus, AgentStatusEnum.ACTIVE.getCode())
                .mapWithKey(AgentEntity::getId);
    }

    @Override
    public void save(AgentEntity agentEntity) {
        agentEntityMapper.save(agentEntity);
    }

    @Override
    public void update(AgentEntity agentEntity) {
        agentEntityMapper.update(agentEntity);
    }

    @Override
    public void delete(Long agentId) {
        DeleteChain.of(agentEntityMapper)
                .eq(AgentEntity::getId, agentId)
                .execute();
    }
}
