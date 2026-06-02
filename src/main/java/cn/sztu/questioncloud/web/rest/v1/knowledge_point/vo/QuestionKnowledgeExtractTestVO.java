package cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionKnowledgeExtractTestVO {
    private Long questionVersionId;
    private Integer total;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long knowledgePointId;
        private String subject;
        private String canonicalName;
        private Integer isMain;
        private Integer relevanceScore;
    }
}
