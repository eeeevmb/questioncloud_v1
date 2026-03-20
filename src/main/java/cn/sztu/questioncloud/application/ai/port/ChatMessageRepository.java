package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;

import java.util.List;

public interface ChatMessageRepository {
    List<ChatMessageEntity> getChatMessagesBySessionId(Long sessionId);

    void batchSave(List<ChatMessageEntity> messages);

    void deleteBySessionId(Long sessionId);
}
