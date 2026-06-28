package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;

import java.util.Map;

public interface AgentRepository {
    Map<Long, AgentEntity> getEntityMap();

    void save(AgentEntity agentEntity);

    void update(AgentEntity agentEntity);

    void delete(Long agentId);
}
