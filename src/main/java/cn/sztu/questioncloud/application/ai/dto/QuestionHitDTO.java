package cn.sztu.questioncloud.application.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * RAG检索命中的题目传输对象
 * 携带足够让大模型选择、追问等的字段
 */
@Data
@Builder
public class QuestionHitDTO {
    /**
     * 相似度排序(topK)
     */
    private Integer matchRank;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题干，截断前200个字符
     */
    private String stemPreview;

    /**
     * 题型编码
     */
    private String typeCode;

    /**
     * 难度系数
     */
    private Double difficulty;


}
