package cn.sztu.questioncloud.infrastructure.common.ai.service;

import cn.sztu.questioncloud.infrastructure.common.ai.constant.CollectionAssistantPrompts;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "questionTool"
)
public interface CollectionAssistantAiService {

    @SystemMessage(CollectionAssistantPrompts.chatTestPrompt)
    Flux<String> chatTest(
            @MemoryId String memoryId,
            @UserMessage String message);
}
