package cn.sztu.questioncloud.web.rest.v1.question.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuestionCreatedVO {
    private Long questionId;

    private Long questionVersionId;

    private Long collectionId;
}
