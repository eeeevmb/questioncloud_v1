package cn.sztu.questioncloud.application.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchQuery {
    /**
     * 查询文本
     */
    private String query;

    /**
     * 最大返回结果数（topK）
     */
    private Integer maxResult;

    /**
     * 最低相似度阈值，范围 [0, 1]。
     */
    private Double minScore;

    /**
     * 元数据过滤条件（可选）
     */
    private SearchFilter filter;
}
