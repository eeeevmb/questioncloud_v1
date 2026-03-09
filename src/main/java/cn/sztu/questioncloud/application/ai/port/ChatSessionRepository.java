package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatSessionEntity;

public interface ChatSessionRepository {
    void save(ChatSessionEntity chatSessionEntity);

    void update(ChatSessionEntity chatSessionEntity);

    void deleteById(Long sessionId);
}
