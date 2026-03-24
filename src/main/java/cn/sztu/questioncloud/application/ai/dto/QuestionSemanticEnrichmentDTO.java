package cn.sztu.questioncloud.application.ai.dto;

import lombok.Data;

import java.util.List;

/**
 * llm返回的题目语义增强结果
 * 用于向量化链路作为输入
 */
@Data
public class QuestionSemanticEnrichmentDTO {
    /**
     * 题干
     */
    private String stem;

    /**
     * 解析
     */
    private String solution;

    /**
     * 知识点标签，后续扩展知识图谱可以改成结构化对象
     */
    private List<String> knowledgePoints;
}
