package cn.sztu.questioncloud.application.paper.enums;

import lombok.Getter;

@Getter
public enum ExamStatusEnum {
    DRAFT(0, "草稿"),
    ACTIVE(1, "考试中"),
    ARCHIVED(2, "已结束");

    private final Integer code;
    private final String description;

    ExamStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
