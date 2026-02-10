package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateQuestionArgs {
    private String typeCode;
    private String title;
    private String stem;

    private List<QuestionOption> options;
    private String answer;
    private List<String> correctOptions;
    private String judgeAnswer;
    private String solution;

    private BigDecimal difficulty = BigDecimal.valueOf(0.50);
    private Long collectionId;
}
