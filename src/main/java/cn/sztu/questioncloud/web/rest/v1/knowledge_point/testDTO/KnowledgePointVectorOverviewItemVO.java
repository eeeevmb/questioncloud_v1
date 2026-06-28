package cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVectorOverviewItemVO {

    private String vectorId;

    private Long knowledgePointId;

    private String subject;

    private String canonicalName;

    private String createdAt;

    private String updatedAt;

    private String document;

    private Map<String, Object> metadata;
}
