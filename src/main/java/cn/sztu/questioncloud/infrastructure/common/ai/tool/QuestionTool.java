package cn.sztu.questioncloud.infrastructure.common.ai.tool;

import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.ToolParamDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.CreateQuestionArgs;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 供LLM调用的题目领域工具函数
 */
@Component
public class QuestionTool {
    private final QuestionAppService questionAppService;

    public static final BigDecimal DEFAULT_DIFF = BigDecimal.valueOf(0.50);

    public QuestionTool(QuestionAppService questionAppService) {
        this.questionAppService = questionAppService;
    }

    @Tool("在题集中添加题目。成功后返回新题目ID(questionId，Long)。")
    public Long createQuestion (
        @P(ToolParamDocs.CREATE_QUESTION_PARAM) CreateQuestionArgs args) {
            BigDecimal difficulty = args.getDifficulty() != null
                ? args.getDifficulty()
                : DEFAULT_DIFF;

            return questionAppService.createQuestion(CreateQuestionReq.builder()
                            .typeCode(args.getTypeCode())
                            .title(args.getTitle())
                            .stem(args.getStem())
                            .options(args.getOptions())
                            .answer(args.getAnswer())
                            .correctOptions(args.getCorrectOptions())
                            .judgeAnswer(args.getJudgeAnswer())
                            .solution(args.getSolution())
                            .difficulty(difficulty)
                            .collectionId(args.getCollectionId())
                            .build()).getQuestionId();
    }
}
