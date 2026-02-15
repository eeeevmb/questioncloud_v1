package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import reactor.core.publisher.Flux;

public interface AgentService {
    /**
     * 测试用聊天接口
     *
     * @param userId 用户ID
     * @param req    聊天请求
     * @return 流式响应
     */
    Flux<String> simpleChat(Long userId, ChatTestReq req);

    /**
     * 与题集小助手对话
     *
     * @param memoryId     会话记忆ID
     * @param collectionId 题集ID
     * @param message      用户消息
     * @return 流式响应
     */
    Flux<String> chatWithAssistant(String memoryId, Long collectionId, String message);

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    ChatSessionVO createNewChatSession(Long userId, Long collectionId);
}
