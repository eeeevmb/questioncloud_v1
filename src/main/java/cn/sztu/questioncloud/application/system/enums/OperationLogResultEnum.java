package cn.sztu.questioncloud.application.system.enums;

import lombok.Getter;

@Getter
public enum OperationLogResultEnum {
    SUCCESS("SUCCESS"),
    FAIL("FAIL");

    private final String code;

    OperationLogResultEnum(String code) {
        this.code = code;
    }
}
