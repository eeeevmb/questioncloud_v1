package cn.sztu.questioncloud.application.ai.port;

import reactor.core.publisher.Flux;

public interface CollectionAssistantChatPort {
    /**
     * 与大模型进行对话
     *
     * @param memoryId 会话记忆ID
     * @param message  用户消息
     * @return 流式响应
     */
    Flux<String> chat(String memoryId, String message);
}
