package cn.sztu.questioncloud.web.rest.v1.paper.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 随机换一题请求参数
 */
@Data
public class RandomReplaceReq {
    @NotEmpty(message = "请至少选择一个题集")
    private List<Long> collectionIds;

    private List<Long> excludedQuestionIds;

    @NotNull(message = "题目类型不能为空")
    private String typeCode;

    @NotNull(message = "单题分值不能为空")
    @DecimalMin(value = "0.5", message = "单题分数不能低于0.5分")
    private BigDecimal score;

    // private Double difficultyMin;
    // private Double difficultyMax;
}