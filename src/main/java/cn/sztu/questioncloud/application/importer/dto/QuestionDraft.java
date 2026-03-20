package cn.sztu.questioncloud.application.importer.dto;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.dto.QuestionOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDraft {
    private String typeCode;
    private String title;
    private String stem;
    private List<QuestionOption> options;
    private String answer;
    private List<String> correctOptions;
    private String judgeAnswer;
    private String solution;
    private BigDecimal difficulty;
}
