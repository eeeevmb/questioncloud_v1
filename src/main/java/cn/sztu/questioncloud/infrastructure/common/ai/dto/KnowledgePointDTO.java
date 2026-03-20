package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI提取的知识点数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointDTO {
    /**
     * 知识点标题（使用学科标准术语）
     */
    @NotBlank(message = "知识点标题不能为空")
    private String title;
    /**
     * 知识点核心定义或简要说明
     */
    private String description;
    /**
     * 别名列表（包含同义词、英文术语、常见缩写）
     */
    private List<String> aliases;
    /**
     * 核心公式的LaTeX表示
     */
    private String formulaOrCode;
    /**
     * 典型例题或应用场景
     */
    private String example;
    /**
     * 重要性权重（1-10）
     */
    private Integer importanceWeight;
}
