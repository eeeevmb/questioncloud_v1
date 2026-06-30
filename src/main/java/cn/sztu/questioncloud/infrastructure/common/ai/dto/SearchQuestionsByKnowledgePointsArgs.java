package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchQuestionsByKnowledgePointsArgs {
    /**
     * 必填，知识点 ID 列表
     */
    private List<Long> knowledgePointIds;

    /**
     * 可选，返回前 K 条题目
     */
    private Integer topK;

    /**
     * 可选，题型过滤
     */
    private String typeCode;

    /**
     * 可选，难度下限
     */
    private Double difficultyMin;

    /**
     * 可选，难度上限
     */
    private Double difficultyMax;
}
