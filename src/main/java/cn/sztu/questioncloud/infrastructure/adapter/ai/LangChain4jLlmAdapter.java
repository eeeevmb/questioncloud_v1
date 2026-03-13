package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.port.LlmPort;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LLM适配器，提供大模型对话能力
 */
@Component
@RequiredArgsConstructor
public class LangChain4jLlmAdapter implements LlmPort {

    private final QwenChatModel chatModel;

    @Override
    public ChatResponse chat(AgentDefinition agent, List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder() // 可以替换成qwen的param
                        .modelName(agent.getChatOptions().getModelName())
                        .temperature(agent.getChatOptions().getTemperature())
                        .maxOutputTokens(agent.getChatOptions().getMaxTokens())
                        .frequencyPenalty(agent.getChatOptions().getFrequencyPenalty())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();

        return chatModel.chat(request);
    }
}
