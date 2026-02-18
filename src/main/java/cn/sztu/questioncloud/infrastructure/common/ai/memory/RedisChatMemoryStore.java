package cn.sztu.questioncloud.infrastructure.common.ai.memory;

import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的 {@link ChatMemoryStore} 实现。
 * 消息列表以 JSON 形式存储到 Redis，并设置 TTL 以控制上下文生命周期，避免缓存无限增长。</p>
 * <p>Key 规则：{@code chat:<memoryId>}</p>
 */
@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    private final CacheService cacheService;
    private static final long MEMORY_TTL_DAYS = 3L;

    public RedisChatMemoryStore(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = cacheService.get(CacheKeyUtil.chatMemoryKey((String) memoryId));
        if (json == null) {
            return List.of();
        } else {
            return ChatMessageDeserializer.messagesFromJson(json);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        cacheService.set(
                CacheKeyUtil.chatMemoryKey(String.valueOf(memoryId)),
                ChatMessageSerializer.messagesToJson(list),
                MEMORY_TTL_DAYS,
                TimeUnit.DAYS);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        cacheService.delete(CacheKeyUtil.chatMemoryKey(String.valueOf(memoryId)));
    }
}
