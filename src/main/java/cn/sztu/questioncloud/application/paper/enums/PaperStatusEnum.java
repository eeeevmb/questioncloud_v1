package cn.sztu.questioncloud.application.paper.enums;

import lombok.Getter;

@Getter
public enum PaperStatusEnum {
    DRAFT(0, "草稿"),
    ACTIVE(1, "启用"),
    ARCHIVED(2, "归档");

    private final Integer code;
    private final String description;

    PaperStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
