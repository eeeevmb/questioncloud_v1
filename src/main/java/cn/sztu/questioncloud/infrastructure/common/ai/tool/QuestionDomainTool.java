package cn.sztu.questioncloud.infrastructure.common.ai.tool;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolParamDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.*;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class QuestionDomainTool {
    private final QuestionAppService questionAppService;
    private final SearchService searchService;

    public static final BigDecimal DEFAULT_DIFF = BigDecimal.valueOf(0.50);

    /**
     * 创建题目工具
     *
     * @param context  业务上下文
     * @param args     题目创建参数
     * @return         工具调用结果
     */
    @Tool(QuestionToolDocs.CREATE_QUESTION)
    public ToolResult<Void> createQuestion (
            ChatSessionContext context,
            @P(QuestionToolParamDocs.CREATE_QUESTION_PARAM) CreateQuestionArgs args) {
        try {
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }
            if (args == null) {
                return new ToolResult<>(false, null, "创建参数不能为空", null);
            }
            // 默认难度兜底
            BigDecimal difficulty = args.getDifficulty() != null
                    ? args.getDifficulty()
                    : DEFAULT_DIFF;
            List<Long> collectionIds = context.getCollectionIds();
            if (collectionIds == null || collectionIds.isEmpty()) {
                return new ToolResult<>(false, null, "当前未绑定题集，无法创建题目", null);
            }
            // 将题目添加进所有选中题集
            for (Long collectionId : collectionIds) {
                questionAppService.createQuestion(CreateQuestionReq.builder()
                        .typeCode(args.getTypeCode())
                        .title(args.getTitle())
                        .stem(args.getStem())
                        .options(args.getOptions())
                        .answer(args.getAnswer())
                        .correctOptions(args.getCorrectOptions())
                        .judgeAnswer(args.getJudgeAnswer())
                        .solution(args.getSolution())
                        .difficulty(difficulty)
                        .collectionId(collectionId)
                        .build(), context.getUserId());
            }

            return new ToolResult<>(true, null, "成功在绑定题集创建题目！", null);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        } catch (Exception e) {
            return new ToolResult<>(false, null, "创建题目失败，请检查参数格式后重试", null);
        }
    }

    /**
     * 获取题目详细信息工具
     * @param context 业务上下文
     * @return 工具调用结果
     */
    @Tool(QuestionToolDocs.GET_QUESTION_DETAIL)
    public ToolResult<List<QuestionDetail>> getQuestionDetail(ChatSessionContext context) {
        try {
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }
            List<Long> questionIds = context.getQuestionIds();
            if (questionIds == null || questionIds.isEmpty()) {
                return new ToolResult<>(true, null, null, List.of());
            }
            List<QuestionDetail> detail = new ArrayList<>();

            // TODO：换成批量查询
            for (Long questionId : questionIds) {
                detail.add(QuestionDetail.fromVO(questionAppService.getQuestionDetailById(questionId, context.getUserId())));
            }

            return new ToolResult<>(true, null, null, detail);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        }
    }

    /**
     * RAG检索题库中的题目
     *
     * @param context 业务上下文
     * @param args    参数列表
     * @return 工具调用结果
     */
    @Tool(QuestionToolDocs.RAG_SEARCH)
    public ToolResult<List<QuestionHitDTO>> searchQuestion(
            ChatSessionContext context,
            @P(QuestionToolParamDocs.RAG_SEARCH_ARGS_PARAM)SearchQuestionArgs args) {
        try {
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }
            if (args == null || args.getQuery() == null || args.getQuery().isBlank()) {
                return new ToolResult<>(false, null, "检索参数 query 不能为空", null);
            }

            List<QuestionHitDTO> data = searchService.searchQuestions(
                    context.getUserId(),
                    context.getCollectionIds(),
                    args.getQuery(),
                    args.getRagSearchParam());
            return new ToolResult<>(true, null, null, data);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        } catch (Exception e) {
            return new ToolResult<>(false, null, "检索失败，请稍后重试", null);
        }
    }
}
