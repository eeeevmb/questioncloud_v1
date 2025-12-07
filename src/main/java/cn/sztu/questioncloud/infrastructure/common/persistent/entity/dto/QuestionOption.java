package cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class QuestionOption implements Serializable {
    /**
     * 选项
     */
    private String key;

    /**
     * 选项内容（文本或LaTex公式）
     */
    private String content;
}
