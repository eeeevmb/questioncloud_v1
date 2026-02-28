package cn.sztu.questioncloud.web.rest.v1.importer.req;

import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ImportItemBatchUpdateReq {
    @NotEmpty(message = "items 不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotNull(message = "itemId 不能为空")
        private Long itemId;

        @NotNull(message = "draft 不能为空")
        private QuestionDraft draft;
    }
}
