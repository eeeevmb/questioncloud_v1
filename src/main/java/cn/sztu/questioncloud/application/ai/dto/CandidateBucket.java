package cn.sztu.questioncloud.application.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CandidateBucket {
    /**
     * 题型
     */
    private String typeCode;

    /**
     * 题目数
     */
    private Integer count;

    /**
     * 考点（作为RAG的关键词）
     */
    private List<String> topics;

    /**
     * 难度下限
     */
    private Double difficultyMin;

    /**
     * 难度上限
     */
    private Double difficultyMax;
}