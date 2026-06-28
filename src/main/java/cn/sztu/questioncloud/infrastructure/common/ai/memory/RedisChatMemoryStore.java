package cn.sztu.questioncloud.infrastructure.common.ai.memory;

import cn.sztu.questioncloud.application.ai.helper.ChatMessageConverter;
import cn.sztu.questioncloud.application.ai.port.ChatMessageRepository;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 {@link ChatMemoryStore} 实现。
 * 消息列表以 JSON 形式存储到 Redis，并设置 TTL 以控制上下文生命周期，避免缓存无限增长。</p>
 * <p>Key 规则：{@code chat:<memoryId>}</p>
 */
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {
    private final ChatMessageRepository chatMessageRepository;
    private final CacheService cacheService;
    private static final long MEMORY_TTL_DAYS = 1L;

    @Override
    public List<ChatMessage> getMessages(Object sessionId) {
        String cache = cacheService.get(CacheKeyUtil.chatMemoryKey((String.valueOf(sessionId))));
        if (cache == null) {
            // 缓存miss
            List<ChatMessage> messages = ChatMessageConverter.toMessages(chatMessageRepository.getChatMessagesBySessionId((Long) sessionId));
            cacheService.set(
                    CacheKeyUtil.chatMemoryKey(String.valueOf(sessionId)),
                    ChatMessageSerializer.messagesToJson(messages),
                    MEMORY_TTL_DAYS,
                    TimeUnit.DAYS);
            return messages;
        } else {
            return ChatMessageDeserializer.messagesFromJson(cache);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)   // 若回滚redis不会同时回滚
    public void updateMessages(Object sessionId, List<ChatMessage> messages) {
        // 清除并落库
        List<ChatMessageEntity> entities = messages.stream()
                .map( (message) -> ChatMessageConverter.toEntity(message, (Long) sessionId))
                .toList();
        chatMessageRepository.deleteBySessionId((Long) sessionId);
        chatMessageRepository.batchSave(entities);

        cacheService.set(
                CacheKeyUtil.chatMemoryKey(String.valueOf(sessionId)),
                ChatMessageSerializer.messagesToJson(messages),
                MEMORY_TTL_DAYS,
                TimeUnit.DAYS);
    }

    @Override
    public void deleteMessages(Object sessionId) {
        chatMessageRepository.deleteBySessionId((Long) sessionId);
        cacheService.delete(CacheKeyUtil.chatMemoryKey(String.valueOf(sessionId)));
    }

//    public void updateMessage(Object sessionId, ChatMessage message) {
//        cacheService.listRightPush(CacheKeyUtil.chatMemoryKey(String.valueOf(sessionId)), message);
//    }
}
