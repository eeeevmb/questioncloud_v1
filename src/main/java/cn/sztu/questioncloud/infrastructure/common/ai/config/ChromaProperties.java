package cn.sztu.questioncloud.infrastructure.common.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chroma")
public class ChromaProperties {
    private String baseUrl;

    private String collection;

    private String apiVersion = "V2";

    private boolean isLogRequests;

    private boolean isLogResponses;
}
