package cn.sztu.questioncloud.web.rest.v1.question.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 *  二次确认请求，用于补充题目信息以及最终落库
 */
public record ConfirmDraftReq(
        @NotNull
        Long collectionId,
        List<Update> updates

) {
    /**
     *  针对某个draft的改动（部分字段可为空，表示不修改）
     */
    public record Update(
            @NotNull(message = "draftId 不能为空")
            Long draftId,
            @NotBlank(message = "题型不能为空")
            String typeCode,
            String title,
            String stem,
            String answer,
            String solution

    ) {}
}

