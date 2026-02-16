package cn.sztu.questioncloud.infrastructure.common.ai.tool;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.QuestionToolParamDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.CreateQuestionArgs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.QuestionDetail;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.ToolResult;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.web.rest.v1.question.req.CreateQuestionReq;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 供LLM调用的题目领域工具函数
 */
@Slf4j
@Component
public class QuestionTool {
    private final QuestionAppService questionAppService;
    private final SearchService searchService;

    public static final BigDecimal DEFAULT_DIFF = BigDecimal.valueOf(0.50);
    private final CacheService cacheService;

    public QuestionTool(QuestionAppService questionAppService, SearchService searchService, CacheService cacheService) {
        this.questionAppService = questionAppService;
        this.searchService = searchService;
        this.cacheService = cacheService;
    }

    /**
     * 创建题目工具
     *
     * @param memoryId 会话记忆ID
     * @param args     题目创建参数
     * @return         工具调用结果
     */
    @Tool(QuestionToolDocs.CREATE_QUESTION)
    public ToolResult<Long> createQuestion (
            @ToolMemoryId String memoryId,
            @P(QuestionToolParamDocs.CREATE_QUESTION_PARAM) CreateQuestionArgs args) {
            // 默认难度兜底
            BigDecimal difficulty = args.getDifficulty() != null
                ? args.getDifficulty()
                : DEFAULT_DIFF;
        try {
            // 从redis获取业务信息
            ChatSessionContext context = cacheService.get(memoryId);
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }

            Long questionId = questionAppService.createQuestion(CreateQuestionReq.builder()
                            .typeCode(args.getTypeCode())
                            .title(args.getTitle())
                            .stem(args.getStem())
                            .options(args.getOptions())
                            .answer(args.getAnswer())
                            .correctOptions(args.getCorrectOptions())
                            .judgeAnswer(args.getJudgeAnswer())
                            .solution(args.getSolution())
                            .difficulty(difficulty)
                            .collectionId(context.getCollectionId())
                            .build(), context.getUserId()).getQuestionId();
            return new ToolResult<>(true, null, null, questionId);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        }
    }

    /**
     * 获取题目详细信息工具
     *
     * @param memoryId 会话记忆ID
     * @return         工具调用结果
     */
    @Tool(QuestionToolDocs.GET_QUESTION_DETAIL)
    public ToolResult<List<QuestionDetail>> getQuestionDetail (
            @ToolMemoryId String memoryId) {
        try {
            // 从redis获取业务信息
            ChatSessionContext context = cacheService.get(memoryId);
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
     * @param memoryId       会话记忆ID
     * @param query          用户查询语句
     * @param ragSearchParam RAG检索参数
     * @return               工具调用结果
     */
    @Tool(QuestionToolDocs.RAG_SEARCH)
    public ToolResult<List<QuestionHitDTO>> searchQuestion(
            @ToolMemoryId String memoryId,
            @P(QuestionToolParamDocs.RAG_SEARCH_PARAM_QUERY) String query,
            @P(QuestionToolParamDocs.RAG_SEARCH_PARAM) RAGSearchParam ragSearchParam) {
        // 从redis获取业务信息
        ChatSessionContext context = cacheService.get(memoryId);
        if (context == null) {
            return new ToolResult<>(false, null, "上下文已过期或不存在", null);
        }

        List<QuestionHitDTO> data = searchService.RAGSearch(context.getUserId(), context.getCollectionId(), query, ragSearchParam);
        return new ToolResult<>(true, null, null, data);
    }
}
