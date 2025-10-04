package cn.sztu.questioncloud.common.constant.enums.result.impl;

import cn.sztu.questioncloud.common.constant.enums.result.ResultCodeEnum;

/**
 * @author LinSanQi
 */

public enum CommonResultCodeEnum implements ResultCodeEnum {

    SUCCESS("200000", "请求成功"),
    PARAM_ERROR("400000", "参数错误"),
    PARAM_VALIDATION_ERROR("400001", "参数验证失败"),
    CANCEL("400002", "请求被取消"),
    TOO_MANY_REQUESTS("429000", "请求过于频繁，请稍后再试"),
    NOT_FOUND("404000", "请求的资源不存在"),
    NOT_LOGIN("401000", "用户未登录"),
    NO_PERMISSION("403000", "无此操作权限"),
    NO_ROLE("403001", "无此角色权限"),
    ERROR("500000", "系统未知错误，请稍后重试"),
    FUTURE_ERROR("500001", "系统错误，请联系管理员"),
    ;

    private final String code;
    private final String message;

    CommonResultCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}