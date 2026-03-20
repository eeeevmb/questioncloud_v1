package cn.sztu.questioncloud.infrastructure.adapter.utils;

import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.service.impl.QuestionAppServiceImpl;
import cn.sztu.questioncloud.common.exception.ApplicationException;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class QuestionUtils {

    public static String getAnswerKey(String typeCode,
                                      List<String> correctOptions,
                                      String judgeAnswer) {
        // 单选
        if (QuestionTypeEnum.SINGLE_CHOICE.getCode().equals(typeCode)) {
            if (correctOptions == null || correctOptions.size() != 1) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "单选题只能有一个正确选项");
            }
            return normalizeOption(correctOptions.getFirst());
        }

        // 多选
        if (QuestionTypeEnum.MULTIPLE_CHOICE.getCode().equals(typeCode)) {
            if (correctOptions == null || correctOptions.isEmpty()) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "多选题至少要有一个正确选项");
            }
            return correctOptions.stream()
                    .filter(Objects::nonNull)
                    .map(QuestionUtils::normalizeOption)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining());
        }

        // 判断
        if (QuestionTypeEnum.TRUE_FALSE.getCode().equals(typeCode)) {
            if (judgeAnswer == null) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "判断题答案不能为空");
            }
            String ans = judgeAnswer.trim().toUpperCase();
            if (!"T".equals(ans) && !"F".equals(ans)) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED,
                        "判断题答案必须为 T 或者 F");
            }
            return ans;
        }

        // 其它题型目前不参与机器判分
        return "";
    }

    private static String normalizeOption(String option) {
        if (option == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_SAVE_FAILED ,"选项值不能为空");
        }
        return option.trim().toUpperCase();
    }
}
