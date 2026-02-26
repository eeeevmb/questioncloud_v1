package cn.sztu.questioncloud.web.rest.v1.paper.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PaperItemBatchSaveReq {
    @NotEmpty(message = "保存的题目列表不能为空")
    private List<PaperItemSaveReq> items;
}
