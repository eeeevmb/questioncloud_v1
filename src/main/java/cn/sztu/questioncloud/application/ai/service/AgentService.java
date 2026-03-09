package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.OldChatSessionVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Agent 应用服务。
 * 对外提供与Agent交互的应用层能力，包括对话、会话创建等。
 * 返回 {@link Flux} 以支持流式输出。
 */
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
     * @param req          聊天请求
     * @return 流式响应
     */
    Flux<String> chatWithAssistant(String memoryId, Long collectionId, AssistantChatReq req);

    /**
     * 获取会话历史
     *
     * @param memoryId 记忆ID
     * @return 会话历史
     */
    List<ChatMessageVO> getChatHistory(String memoryId);

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    @Deprecated
    OldChatSessionVO createNewChatSession(Long userId, Long collectionId);

    /**
     * 创建新智能体会话
     *
     * @param userId    用户ID
     * @param agentName 智能体名称
     * @return 会话视图
     */
    ChatSessionVO createNewChatSession(Long userId, String agentName);
}
