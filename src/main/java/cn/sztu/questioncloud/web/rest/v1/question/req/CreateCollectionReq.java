package cn.sztu.questioncloud.web.rest.v1.question.req;

import jakarta.validation.constraints.NotBlank;

public record CreateCollectionReq(
        @NotBlank(message = "题集名不能为空")
        String name,
        String description
) {}
