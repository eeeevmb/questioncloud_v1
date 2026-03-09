package cn.sztu.questioncloud.application.ai.enums;

import lombok.Getter;

@Getter
public enum AgentStatusEnum {
    DISABLE(0, "禁用"),
    ACTIVE(1, "启用");

    private final Integer code;
    private final String description;

    AgentStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
