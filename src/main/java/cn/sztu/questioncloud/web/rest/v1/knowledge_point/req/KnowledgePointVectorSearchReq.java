package cn.sztu.questioncloud.web.rest.v1.knowledge_point.req;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgePointVectorSearchReq {
    private List<String> knowledgeScopes;
    private String query;
    private Integer topK;
    private Double minScore;
}
