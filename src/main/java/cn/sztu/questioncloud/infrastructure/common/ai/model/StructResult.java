package cn.sztu.questioncloud.infrastructure.common.ai.model;

import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StructResult<T> {
    private String content;
    private T result;
    private TokenUsage tokenUsage;

    private static <T> StructResult<T> of(String content, T result, TokenUsage tokenUsage) {
        return new StructResult<T>(content, result, tokenUsage);
    }
}
