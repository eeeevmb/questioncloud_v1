package cn.sztu.questioncloud.web.rest.v1.ai.req;

import lombok.Data;

@Data
public class GenerateQuestionDraftReq {
    /** 知识点 / 出题方向，必填 */
    private String topic;

    /** 题型：single-choice / multiple-choice / true-false / fill-in / short-answer */
    private String typeCode;

    /** 适用场景，可选：课堂练习 / 考试题 / 面试题 / 训练题 */
    private String scene;

    /** 额外要求，可选 */
    private String extraRequirements;

}