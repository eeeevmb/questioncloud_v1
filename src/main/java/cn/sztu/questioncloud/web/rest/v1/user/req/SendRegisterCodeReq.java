package cn.sztu.questioncloud.web.rest.v1.user.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * 发送注册验证码请求
 */
public record SendRegisterCodeReq(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email
) {}
