package cn.sztu.questioncloud.web.rest.v1.paper.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaperUpdateReq {
    @NotBlank(message = "试卷标题不能为空")
    private String title;

    private String description;
}
