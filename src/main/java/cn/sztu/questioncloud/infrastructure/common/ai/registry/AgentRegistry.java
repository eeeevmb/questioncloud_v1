package cn.sztu.questioncloud.infrastructure.common.ai.registry;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.spi.QwenChatModelBuilderFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;

public class AgentRegistry {
    ChatModel chatModel = QwenChatModel.builder()
            .build();
}
