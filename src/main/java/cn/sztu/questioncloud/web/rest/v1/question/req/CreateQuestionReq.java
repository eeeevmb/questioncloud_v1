package cn.sztu.questioncloud.web.rest.v1.question.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateQuestionReq(
        @NotBlank(message = "题目类型不能为空")
        String typeCode,

        @NotBlank(message = "题目标题不能为空")
        String title,

        @NotBlank(message = "题干不能为空")
        String stem,

        String answer,

        String solution,

        @NotNull(message = "必须选择一个题集")
        Long collectionId
) {}
