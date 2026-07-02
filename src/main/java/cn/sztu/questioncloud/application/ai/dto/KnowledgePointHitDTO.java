package cn.sztu.questioncloud.application.ai.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointHitDTO {
    /**
     * 相似度排序
     */
    private Integer similarityRank;

    /**
     * 知识点名称
     */
    private String canonicalName;

    /**
     * 知识点主键，供后续工具继续引用
     */
    private Long knowledgePointId;

    /**
     * 命中的知识点领域列表
     */
    private List<String> scopeNames;
}
