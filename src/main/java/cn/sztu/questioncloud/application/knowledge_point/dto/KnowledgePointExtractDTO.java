package cn.sztu.questioncloud.application.knowledge_point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointExtractDTO {
    /**
     * 学科/科目
     */
    private String subject;

    /**
     * 标准知识点名称。
     */
    private String canonicalName;

    /**
     * 是否为该题主要考点。
     * 0=次要考点，1=主要考点。
     */
    private Integer isMain;

    /**
     * 与当前题目的相关度，0-100。
     */
    private Integer relevanceScore;
}
