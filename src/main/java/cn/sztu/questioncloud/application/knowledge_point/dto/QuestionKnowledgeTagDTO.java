package cn.sztu.questioncloud.application.knowledge_point.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionKnowledgeTagDTO {

    private Long knowledgePointId;

    private String canonicalName;

    private Integer isMain;

    private Integer relevanceScore;
}
