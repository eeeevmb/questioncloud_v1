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
 * 随机换一题请求参数
 */
@Data
public class RandomReplaceReq {
    @NotEmpty(message = "请至少选择一个题集")
    private List<Long> collectionIds;

    private List<Long> excludedQuestionIds;

    @NotNull(message = "题目类型不能为空")
    private String typeCode;

    @Schema(description = "目标难度 0~1，可为空，为空则不进行难度加权")
    @DecimalMax(value = "1.00", inclusive = true, message = "难度不能大于 1.00")
    @DecimalMin(value = "0.00", inclusive = true, message = "难度不能小于 0.00")
    private Double expectedDifficulty;
}