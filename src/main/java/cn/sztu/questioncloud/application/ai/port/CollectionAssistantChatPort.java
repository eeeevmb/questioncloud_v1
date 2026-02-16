package cn.sztu.questioncloud.application.ai.port;

import reactor.core.publisher.Flux;

/**
 * 题集小助手对话端口。
 * 定义面向应用层的大模型对话能力抽象。
 * 接口方法返回 {@link Flux} 以支持流式输出（如 SSE）。
 */
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
