package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import reactor.core.publisher.Flux;

public interface AgentChatService {
    /**
     * 聊天链路
     * @param context
     * @param sessionId
     * @param agentName
     * @param userInput
     * @return
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
}
