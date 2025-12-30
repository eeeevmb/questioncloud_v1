package cn.sztu.questioncloud.web.rest.v1.paper.req;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaperItemSaveReq {
    @NotNull(message = "题目ID不能为空")
    private Long questionId;

    @NotNull(message = "题目版本ID不能为空")
    private Long questionVersionId;

    @NotNull(message = "分数不能为空")
    @DecimalMin(value = "0.5", message = "单题分数不能低于0.5分")
    private BigDecimal score;

    // --- 备注 ---
    // 题序由前端统一返回数组的顺序决定
}
