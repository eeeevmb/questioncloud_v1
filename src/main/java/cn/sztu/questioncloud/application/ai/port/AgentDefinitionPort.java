package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;

/**
 * 智能体定义获取端口
 */
public interface AgentDefinitionPort {
    AgentDefinition findByName(String agentName);
}
