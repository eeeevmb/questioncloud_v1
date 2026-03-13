package cn.sztu.questioncloud.application.ai.dto;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedKnowledgeBase;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedTool;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ChatOptions;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 预设智能体配置，提供预设的系统提示词，可用工具、知识库等
 */
@Data
@Builder
public class AgentDefinition {
    /**
     * 智能体名称，目前有：题库小助手、智能组卷助手
     */
    private String name;

    /**
     * 预设系统提示词
     */
    private String systemPrompt;

    /**
     * 预设可用工具，限定智能体能力范围
     */
    private List<AllowedTool> allowedTools;

    /**
     * 预设可用知识库，目前知识库模块尚未实现
     */
    private List<AllowedKnowledgeBase> allowedKnowledgeBases;

    /**
     * 预设模型参数配置
     */
    private ChatOptions chatOptions;

    public static AgentDefinition fromEntity(AgentEntity agentEntity) {
        return AgentDefinition.builder()
                .name(agentEntity.getName())
                .systemPrompt(agentEntity.getSystemPrompt())
                .allowedTools(agentEntity.getAllowedTools())
                .allowedKnowledgeBases(agentEntity.getAllowedKnowledgeBases())
                .chatOptions(agentEntity.getChatOptions())
                .build();
    }
}

