package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;

/**
 * 大语言模型端口，提供外部模型能力
 */
public interface LlmPort {
    // 同步接口
    ChatResponse chat(AgentDefinition agent,
                      List<ChatMessage> messages,
                      List<ToolSpecification> toolSpecifications);

    // 流式接口
}
