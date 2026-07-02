package cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointVO {
    private Long knowledgePointId;
    private String canonicalName;
}
