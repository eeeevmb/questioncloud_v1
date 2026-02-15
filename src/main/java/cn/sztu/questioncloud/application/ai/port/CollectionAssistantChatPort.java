package cn.sztu.questioncloud.application.ai.port;

import reactor.core.publisher.Flux;

public interface CollectionAssistantChatPort {
    /**
     * 与大模型进行对话测试
     *
     * @param memoryId 会话记忆ID
     * @param message  用户消息
     * @return 流式响应
     */
    Flux<String> chatTest(String memoryId, String message);

    /**
     * 与题集小助手对话
     *
     * @param memoryId     会话记忆ID
     * @param collectionId 题集ID
     * @param message      用户消息
     * @return 流式响应
     */
    Flux<String> chatWithAssistant(String memoryId, Long collectionId, String message);
}
