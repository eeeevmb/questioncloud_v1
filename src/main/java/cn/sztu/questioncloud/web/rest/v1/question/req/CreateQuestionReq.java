package cn.sztu.questioncloud.web.rest.v1.question.req;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionAsset;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateQuestionReq {
    @NotBlank(message = "题目类型不能为空")
    private String typeCode;

    @NotBlank(message = "题目标题不能为空")
    private String title;

    @NotBlank(message = "题干不能为空")
    private String stem;

    private List<QuestionOption> options;

    // answer用于展示，correctOptions与judgeAnswer用于判分
    private String answer;

    private List<String> correctOptions;

    private String judgeAnswer;

    private String solution;

    @DecimalMin(value = "0.00", inclusive = true, message = "难度不能小于 0.00")
    @DecimalMax(value = "1.00", inclusive = true, message = "难度不能大于 1.00")
    private BigDecimal difficulty;

    @NotNull(message = "必须选择一个题集")
    private Long collectionId;

    private List<QuestionAsset> assets;
}
