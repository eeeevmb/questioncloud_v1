package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AgentChatService {
    /**
     * Agent 对话主链路。
     * <p>
     * 该方法会在指定会话内执行 think-execute 循环：
     * 先由思考模型判断是否需要调用工具，若需要则执行工具并回填结果，
     * 直至无需继续调用工具后，再由主模型流式生成最终回复。
     *
     * @param context   会话业务上下文（如题集范围、已选题目）
     * @param sessionId 聊天会话ID
     * @param agentName 智能体名称
     * @param userInput 用户输入
     * @return 流式文本分片（由上层封装为 SSE message 事件）
     */
    Flux<String> chat(ChatSessionContext context, Long sessionId, String agentName, String userInput);

    /**
     * 创建新智能体会话
     *
     * @param userId    用户ID
     * @param agentName 智能体名称
     * @return 会话视图
     */
    ChatSessionVO createNewChatSession(Long userId, String agentName);

    /**
     * 获取聊天会话列表
     *
     * @param userId 用户ID
     * @return 会话视图列表
     */
    List<ChatSessionVO> getChatSessions(Long userId);

    /**
     * 更新会话标题
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param title 新标题
     */
    void updateChatSessionTitle(Long userId, Long sessionId, String title);

    /**
     * 硬删除单个会话
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    void deleteChatSession(Long userId, Long sessionId);

    /**
     * 获取会话的聊天记录
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 聊天记录视图
     */
    List<ChatMessageVO> getChatMessages(Long userId, Long sessionId);
}
