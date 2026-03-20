package cn.sztu.questioncloud.config;

import cn.sztu.questioncloud.infrastructure.common.ai.config.ChromaProperties;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaApiVersion;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChromaConfig {
    private final ChromaProperties chromaProperties;

    public ChromaConfig(ChromaProperties chromaProperties) {
        this.chromaProperties = chromaProperties;
    }

    @Bean
    public EmbeddingStore<TextSegment> chromaEmbeddingStore() {
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaProperties.getBaseUrl())
                .apiVersion(ChromaApiVersion.valueOf(chromaProperties.getApiVersion()))
                .collectionName(chromaProperties.getCollection())
                .logRequests(chromaProperties.isLogRequests())
                .logResponses(chromaProperties.isLogResponses())
                .build();
    }
}
