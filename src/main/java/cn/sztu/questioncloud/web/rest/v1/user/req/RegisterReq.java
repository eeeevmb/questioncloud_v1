package cn.sztu.questioncloud.web.rest.v1.user.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// TODO 邮箱注册

/**
 * 用户注册请求
 * @param username
 * @param password
 */

public record RegisterReq (

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[\\p{IsHan}a-zA-Z0-9_-]{3,16}$",
                message = "用户名必须是3-16个字符，可以包含中文、字母、数字、下划线和连字符")
        String username,

        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,

        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z\\d@$!%*?&#.]{8,20}$)(?=(.*[A-Z].*)|(.*[a-z].*)|(.*\\d.*)|(.*[@$!%*?&#.].*)){2}.*$",
                message = "密码必须为8-20个字符，并包含至少两种以下类型：大写字母、小写字母、数字、特殊字符(@$!%*?&#.)")
        String password
){}
