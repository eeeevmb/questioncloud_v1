package cn.sztu.questioncloud.infrastructure.common.ai.dto;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuestionDetail {
    private Long questionId;
    private String typeCode;
    private String title;
    private String stem;
    private List<QuestionOption> options;
    private String answer;
    private String gradingAnswer;
    private String solution;
    private Double difficulty;

    public static QuestionDetail fromVO(QuestionDetailVO VO) {
        return QuestionDetail.builder()
                .questionId(VO.getId())
                .typeCode(VO.getTypeCode())
                .title(VO.getTitle())
                .stem(VO.getStem())
                .options(VO.getOptions())
                .answer(VO.getAnswer())
                .gradingAnswer(VO.getAnswerKey())
                .solution(VO.getSolution())
                .difficulty(VO.getDifficulty())
                .build();
    }
}