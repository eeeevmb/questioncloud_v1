package cn.sztu.questioncloud.infrastructure.common.ai.memory;

import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisChatMemoryStore implements ChatMemoryStore {
    private final CacheService cacheService;
    private static final long MEMORY_TTL_DAYS = 3L;
    private static final String KEY_PREFIX = "chat:";

    public RedisChatMemoryStore(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    private static String key(Object memoryId) {
        return KEY_PREFIX + memoryId;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String json = cacheService.get(key(memoryId));
        if (json == null) {
            return List.of();
        } else {
            return ChatMessageDeserializer.messagesFromJson(json);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        cacheService.set(
                key((memoryId)),
                ChatMessageSerializer.messagesToJson(list),
                MEMORY_TTL_DAYS,
                TimeUnit.DAYS);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        cacheService.delete(key(memoryId));
    }
}
