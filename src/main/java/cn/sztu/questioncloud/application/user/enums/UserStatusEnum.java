package cn.sztu.questioncloud.application.user.enums;

import lombok.Getter;

@Getter
public enum UserStatusEnum {
    BANNED(0,"禁用"),
    ACTIVE(1, "正常");

    private final int code;
    private final String description;

    UserStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
