package cn.sztu.questioncloud.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    private final ChatMemoryStore redisChatMemoryStore;

    private static final Integer MAX_WINDOW_SIZE = 20;

    public AiConfig(ChatMemoryStore redisChatMemoryStore) {
        this.redisChatMemoryStore = redisChatMemoryStore;
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider() {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(MAX_WINDOW_SIZE)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }
}
