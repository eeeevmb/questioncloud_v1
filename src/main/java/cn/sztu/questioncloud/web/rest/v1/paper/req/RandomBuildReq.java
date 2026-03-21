package cn.sztu.questioncloud.web.rest.v1.paper.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 随机抽取题目请求规则
 */
@Data
public class RandomBuildReq {
    @NotEmpty(message = "请至少选择一个题集")
    private List<Long> collectionIds;

    @NotEmpty(message = "请配置组卷规则")
    private List<Rule> rules;

    @Data
    public static class Rule {
        @NotNull(message = "题目类型不能为空")
        private String typeCode; // 例如: SINGLE_CHOICE, SHORT_ANSWER

        @NotNull(message = "题目数量不能为空")
        private Integer count;   // 例如: 5道

        @Schema(description = "目标难度 0~1，可为空，为空则不进行难度加权")
        @DecimalMin(value = "0.00", inclusive = true, message = "难度不能小于 0.00")
        @DecimalMax(value = "1.00", inclusive = true, message = "难度不能大于 1.00")
        private Double expectedDifficulty;
    }
}
