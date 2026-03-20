package cn.sztu.questioncloud.application.rag.enums;

import lombok.Getter;

@Getter
public enum KnowledgeSourceTypeEnum {
    MANUAL(0, "人工添加"),
    AI_EXTRACTED(1,"AI提炼");

    private final Integer code;
    private final String description;

    KnowledgeSourceTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
