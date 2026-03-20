package cn.sztu.questioncloud.web.rest.v1.ai.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateChatSessionTitleReq {
    @NotBlank(message = "会话标题不能为空")
    @Size(max = 20, message = "会话标题长度不能超过20个字符")
    private String title;
}
