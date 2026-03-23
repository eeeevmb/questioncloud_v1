package cn.sztu.questioncloud.infrastructure.common.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "langchain4j.community.dashscope")
public class LlmModelProperties {

    private ModelConfig chatModel;

    private ModelConfig streamingChatModel;

    private EmbeddingModelConfig embeddingModel;

    @Data
    public static class ModelConfig {
        private String apiKey;
        private String modelName;
        private String baseUrl;
    }

    @Data
    public static class EmbeddingModelConfig {
        private String apiKey;
        private String modelName;
    }

}
