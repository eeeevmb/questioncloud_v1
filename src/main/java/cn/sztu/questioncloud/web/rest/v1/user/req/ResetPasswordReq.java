package cn.sztu.questioncloud.web.rest.v1.user.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 重置密码请求
 * @param email 用户邮箱
 * @param verificationCode 验证码
 * @param newPassword 新密码
 */
public record ResetPasswordReq(

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码必须为6位数字")
        String verificationCode,

        @NotBlank(message = "新密码不能为空")
        @Pattern(
                regexp = "^(?=.*[A-Za-z\\d@$!%*?&#.]{8,20}$)(?=(.*[A-Z].*)|(.*[a-z].*)|(.*\\d.*)|(.*[@$!%*?&#.].*)){2}.*$",
                message = "密码必须是8-20个字符，并且至少包含两种类型"
        )
        String newPassword

) {}
