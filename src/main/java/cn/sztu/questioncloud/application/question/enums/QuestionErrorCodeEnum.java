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
    QUESTION_TYPE_ERROR("Q00004", "题型不存在"),
    QUESTION_ASSET_NOT_FOUND("Q00005", "附件不存在"),
    // ===== 题集相关错误码 =====
    COLLECTION_NOT_FOUND("Q10001", "题集不存在"),
    QUESTION_QUANTITY_INSUFFICIENT("Q10002", "题库余量不足"),
    COLLECTION_NAME_CONFLICT("Q10003", "题集名称已存在"),
    // ===== 批量导入相关错误码 =====
    IMPORT_SESSION_NOT_FOUND("Q20001", "会话不存在"),
    IMPORT_CONTAINS_INVALID_ITEMS("Q20002", "部分题目信息不完整或格式错误"),
    IMPORT_NO_VALID_ITEM("Q20003", "无可导入的题目草稿"),
    IMPORT_SESSION_COMMITTING("Q20004", "导入中，请稍等"),
    IMPORT_SESSION_PARSING("Q20005", "文件解析中，请稍等"),
    IMPORT_SESSION_CANCELED("Q20006", "批量导入已取消，请重新导入"),
    IMPORT_SESSION_FAILED("Q20007", "批量导入失败，请检查上传的模版是否有误"),
    IMPORT_SESSION_CANNOT_CANCEL("Q20008", "当前导入会话无法取消");

    private final String code;
    private final String message;

    QuestionErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
