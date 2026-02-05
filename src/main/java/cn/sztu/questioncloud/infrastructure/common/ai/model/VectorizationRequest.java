package cn.sztu.questioncloud.infrastructure.common.ai.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorizationRequest {

    /**
     * 需要向量化的文本内容
     */
    private String text;

    /**
     * 附加元数据
     * eg. Map.fromCode("user_id", "1", "question_id", "2")
     */
    private Map<String, String> metadata;
}
