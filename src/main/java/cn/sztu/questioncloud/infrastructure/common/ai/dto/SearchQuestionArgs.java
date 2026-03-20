package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import lombok.Data;

@Data
public class SearchQuestionArgs {
    private String query;
    private RAGSearchParam ragSearchParam;
}
