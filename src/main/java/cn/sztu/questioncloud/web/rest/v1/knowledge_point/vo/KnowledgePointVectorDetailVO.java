package cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVectorDetailVO {
    private Long knowledgePointId;
    private String vectorId;
    private String collectionId;
    private String collectionName;
    private String subject;
    private String canonicalName;
    private String document;
    private Map<String, Object> metadata;
}
