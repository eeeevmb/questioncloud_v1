package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.port.ChatMessageRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent.ChatMessageEntityMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final ChatMessageEntityMapper chatMessageEntityMapper;

    @Override
    public List<ChatMessageEntity> getChatMessagesBySessionId(Long sessionId) {
        return QueryChain.of(chatMessageEntityMapper)
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .orderBy(ChatMessageEntity::getCreatedAt)
                .list();
    }

    @Override
    public void batchSave(List<ChatMessageEntity> messages) {
        chatMessageEntityMapper.saveBatch(messages);
    }

    @Override
    public void deleteBySessionId(Long sessionId) {
        DeleteChain.of(chatMessageEntityMapper)
                .eq(ChatMessageEntity::getSessionId, sessionId)
                .execute();
    }
}
