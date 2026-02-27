package cn.sztu.questioncloud.web.rest.v1.question.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchCreateCollectionsReq(
        @NotEmpty(message = "批量创建的题集列表不能为空")
        @Valid
        List<CreateCollectionReq> collections
) {}
