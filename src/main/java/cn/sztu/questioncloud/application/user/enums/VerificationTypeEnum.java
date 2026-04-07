package cn.sztu.questioncloud.application.user.enums;

import lombok.Getter;

@Getter
public enum VerificationTypeEnum {

    REGISTER("register", 5, "用户注册"),
    PASSWORD_RESET("password_reset", 5, "重置密码");

    private final String value;
    private final int expiryMinutes; //验证码过期时间，单位：分钟
    private final String description;

    VerificationTypeEnum(String value, int expiryMinutes, String description) {
        this.value = value;
        this.expiryMinutes = expiryMinutes;
        this.description = description;
    }
}