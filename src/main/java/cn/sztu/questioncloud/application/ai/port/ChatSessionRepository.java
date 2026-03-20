package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatSessionEntity;

import java.util.List;

public interface ChatSessionRepository {
    void save(ChatSessionEntity chatSessionEntity);

    ChatSessionEntity findById(Long id);

    List<ChatSessionEntity> findByUserId(Long userId);

    void update(ChatSessionEntity chatSessionEntity);

    void deleteById(Long sessionId);
}
