package cn.sztu.questioncloud.web.rest.v1.question.req;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionAsset;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UpdateQuestionReq {
    private String title;

    @NotBlank(message = "题干不能为空")
    private String stem;

    private List<QuestionOption> options;

    private String answer;

    private List<String> correctOptions;

    private String judgeAnswer;

    private String solution;

    private List<QuestionAsset> assets;
}
