package cn.sztu.questioncloud.application.question.enums;

import lombok.Getter;

/**
 * 题集来源类型枚举
 *
 * @author eeeevmb
 */
@Getter
public enum CollectionSourceEnum {
    USER(0, "用户创建"),
    SYSTEM(1, "系统创建");

    private final int code;
    private final String description;

    CollectionSourceEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
