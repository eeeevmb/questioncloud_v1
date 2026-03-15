package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;

public interface ChatSessionContextPort {
    /**
     * 在缓存中写入业务上下文
     *
     * @param sessionId          会话ID
     * @param chatSessionContext 上下文
     */
    void saveContext(Long sessionId, ChatSessionContext chatSessionContext);
}
