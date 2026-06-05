package cn.sztu.questioncloud.web.rest.v1.knowledge_point.req;

import lombok.Data;

@Data
public class KnowledgePointVectorSearchReq {
    private String subject;
    private String query;
    private Integer topK;
    private Double minScore;
}
