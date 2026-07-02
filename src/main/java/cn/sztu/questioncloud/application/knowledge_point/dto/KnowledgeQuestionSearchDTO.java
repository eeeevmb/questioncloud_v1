package cn.sztu.questioncloud.application.knowledge_point.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class KnowledgeQuestionSearchDTO {

    private Long questionId;

    private Long questionVersionId;

    private String title;

    private String stemPreview;

    private String typeCode;

    private Double difficulty;

    private Double exposureFactor;

    private String fromCollectionName;

    private Integer matchedKnowledgePointCount;

    private Integer mainKnowledgePointCount;

    private Integer bestRelevanceScore;

    private List<Long> matchedKnowledgePointIds;
}
