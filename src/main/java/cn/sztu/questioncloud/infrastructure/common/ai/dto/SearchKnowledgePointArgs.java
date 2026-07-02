package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchKnowledgePointArgs {
    /**
     * 必填，知识点搜索词
     */
    private String query;

    /**
     * 可选，限定知识点领域列表
     */
    private List<String> knowledgeScopes;

    /**
     * 可选，返回前K个
     */
    private Integer topK;

    /**
     * 可选，最低相似度
     */
    private Double minScore;
}
