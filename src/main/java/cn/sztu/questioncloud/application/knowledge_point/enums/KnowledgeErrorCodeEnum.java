package cn.sztu.questioncloud.application.knowledge_point.enums;

import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;
import lombok.Getter;

/**
 * 知识点模块错误码
 */
@Getter
public enum KnowledgeErrorCodeEnum implements ResultCodeEnum {
    KNOWLEDGE_SCOPE_NOT_FOUND("K00001", "知识点领域不存在"),
    TEXTBOOK_ALREADY_EXISTS("K00002", "教材已存在");

    private final String code;
    private final String message;

    KnowledgeErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
