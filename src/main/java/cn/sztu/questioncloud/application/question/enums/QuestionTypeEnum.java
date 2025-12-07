package cn.sztu.questioncloud.application.question.enums;

import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import lombok.Getter;

import java.util.Arrays;

/**
 * 题型定义，由应用层枚举维护，避免依赖数据库字典。
 *
 * @author Codex
 */
@Getter
public enum QuestionTypeEnum {
    SINGLE_CHOICE("single-choice", "单选题"),
    MULTIPLE_CHOICE("multiple-choice", "多选题"),
    TRUE_FALSE("true-false", "判断题"),
    FILL_IN_BLANK("fill-in", "填空题"),
    SHORT_ANSWER("short-answer", "简答题");

    private final String code;
    private final String description;

    QuestionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 校验题型编码是否受支持，非法则返回false。
     *
     * @param code 题型编码
     * @return 是否支持
     */
    public static boolean ensureValid(String code) {
        if (code == null) {
            return false;
        }
        boolean exists = Arrays.stream(values())
                .anyMatch(type -> type.code.equals(code));
        if (!exists) {
            return false;
        }
        return true;
    }
}
