package cn.sztu.questioncloud.infrastructure.common.ai.model;

import dev.langchain4j.model.output.TokenUsage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerationResult <T> {
    private T result;

    private TokenUsage tokenUsage;
}
