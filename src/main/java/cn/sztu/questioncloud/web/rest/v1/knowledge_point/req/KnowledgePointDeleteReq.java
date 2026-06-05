package cn.sztu.questioncloud.web.rest.v1.knowledge_point.req;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgePointDeleteReq {
    private List<Long> knowledgePointIds;
}
