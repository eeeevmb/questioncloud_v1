package cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVectorSearchVO {
    private Long knowledgePointId;
    private String subject;
    private String canonicalName;
    private Double score;
    private String vectorId;

    /**
     * 向量库中保存的原始文本片段，仅测试/调试时使用。
     */
    private String text;
}