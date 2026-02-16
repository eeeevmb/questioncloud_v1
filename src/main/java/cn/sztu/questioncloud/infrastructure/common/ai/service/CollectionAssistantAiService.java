package cn.sztu.questioncloud.infrastructure.common.ai.service;

import cn.sztu.questioncloud.infrastructure.common.ai.constant.CollectionAssistantPrompts;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

/**
 * 题集小助手 AI 服务。
 *
 * <p>基于 LangChain4j 的 {@link AiService} 声明式接口，将系统提示词、会话记忆、工具调用与流式模型绑定到方法上。
 * 方法返回 {@link Flux} 以支持流式输出（如 SSE）。</p>
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        tools = "questionTool"
)
public interface CollectionAssistantAiService {

    @SystemMessage(CollectionAssistantPrompts.CHAT_TEST_PROMPT)
    Flux<String> chatTest(
            @MemoryId String memoryId,
            @UserMessage String message);

    @SystemMessage(CollectionAssistantPrompts.ASSISTANT_PROMPT)
    Flux<String> chatWithAssistant(
            @MemoryId String memoryId,
            @V("collectionId") Long collectionId,
            @UserMessage String message);
}
