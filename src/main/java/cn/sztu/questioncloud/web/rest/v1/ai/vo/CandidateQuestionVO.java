package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateQuestionVO {
    private Long questionId;

    private Long questionVersionId;

    private String title;

    private String typeCode;

    private Double difficulty;
}
