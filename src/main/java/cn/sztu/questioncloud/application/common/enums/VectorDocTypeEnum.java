package cn.sztu.questioncloud.application.common.enums;

import lombok.Getter;

@Getter
public enum VectorDocTypeEnum {
    QUESTION("QUESTION"),
    KNOWLEDGE_POINT("KNOWLEDGE_POINT");

    private final String code;

    VectorDocTypeEnum(String code) {
        this.code = code;
    }
}