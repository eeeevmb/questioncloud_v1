package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.port.AgentDefinitionPort;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent.AgentEntityMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AgentDefinitionAdapter implements AgentDefinitionPort {
    private final AgentEntityMapper agentEntityMapper;

    @Override
    public AgentDefinition findByName(String agentName) {
        return AgentDefinition.fromEntity(QueryChain.of(agentEntityMapper)
                .eq(AgentEntity::getName, agentName)
                .get());
    }
}
