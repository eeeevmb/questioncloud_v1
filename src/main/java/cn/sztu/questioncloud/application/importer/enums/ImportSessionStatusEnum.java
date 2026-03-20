package cn.sztu.questioncloud.application.importer.enums;

import lombok.Getter;

@Getter
public enum ImportSessionStatusEnum {
    PARSING(0, "解析中"),
    READY(1, "准备提交"),
    COMMITTING(2, "提交中"),
    COMMITTED(3, "提交完毕"),
    CANCELED(4, "取消导入"),
    FAILED(5, "导入失败");

    private final Integer code;
    private final String description;

    ImportSessionStatusEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
