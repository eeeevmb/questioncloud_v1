package cn.sztu.questioncloud.application.question.enums;

import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;
import lombok.Getter;

/**
 * 题目模块错误码
 */
@Getter
public enum QuestionErrorCodeEnum implements ResultCodeEnum {
    // ===== 题目相关错误码 =====
    QUESTION_NOT_FOUND("Q00001", "题目不存在"),
    QUESTION_SAVE_FAILED("Q00002", "保存题目失败"),
    QUESTION_ASSET_TYPE_NOT_ALLOWED("Q00003", "不允许的附件类型"),
    IMPORT_SESSION_NOT_FOUND("Q00004", "会话不存在"),
    QUESTION_TYPE_ERROR("Q00005", "题型不存在"),
    QUESTION_ASSET_NOT_FOUND("Q00006", "附件不存在"),
    // ===== 题集相关错误码 =====
    COLLECTION_NOT_FOUND("Q10001", "题集不存在");

    private final String code;
    private final String message;

    QuestionErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
