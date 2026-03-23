package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 从用户自然语言提取的题目筛选参数
 */
@Data
@Builder
public class RAGSearchParam {
    private Double difficultyMin;

    private Double difficultyMax;

    private String typeCode;
}
