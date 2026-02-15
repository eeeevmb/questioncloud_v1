package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.Data;

/**
 * 从用户自然语言提取的题目筛选参数
 */
@Data
public class RAGSearchParam {
    private Double difficultyMin;

    private Double difficultyMax;

    private String typeCode;
}
