package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.port.ChatSessionRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatSessionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.agent.ChatSessionEntityMapper;
import cn.xbatis.core.sql.executor.chain.DeleteChain;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepository {
    private final ChatSessionEntityMapper chatSessionEntityMapper;

    @Override
    public void save(ChatSessionEntity chatSessionEntity) {
        chatSessionEntityMapper.save(chatSessionEntity);
    }

    @Override
    public ChatSessionEntity findById(Long id) {
        return chatSessionEntityMapper.getById(id);
    }

    @Override
    public List<ChatSessionEntity> findByUserId(Long userId) {
        return QueryChain.of(chatSessionEntityMapper)
                .eq(ChatSessionEntity::getUserId, userId)
                .list();
    }

    @Override
    public void update(ChatSessionEntity chatSessionEntity) {
        chatSessionEntityMapper.update(chatSessionEntity);
    }

    @Override
    public void deleteById(Long sessionId) {
        DeleteChain.of(chatSessionEntityMapper)
                .eq(ChatSessionEntity::getId, sessionId)
                .execute();
    }
}
