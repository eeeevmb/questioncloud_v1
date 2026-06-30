package cn.sztu.questioncloud.infrastructure.common.ai.tool;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.dto.KnowledgePointHitDTO;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeToolDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeToolParamDocs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.SearchKnowledgePointArgs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.SearchQuestionsByKnowledgePointsArgs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.ToolResult;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KnowledgePointTool {
    private static final Integer DEFAULT_TOP_K = 30;
    private static final Double DEFAULT_MIN_SCORE = 0.0;

    private final SearchService searchService;

    /**
     * 向量检索知识点候选
     *
     * @param context 会话上下文
     * @param args    检索参数
     * @return 工具调用结果
     */
    @Tool(KnowledgeToolDocs.SEARCH_KNOWLEDGE_POINTS)
    public ToolResult<List<KnowledgePointHitDTO>> searchKnowledgePoints(
            ChatSessionContext context,
            @P(KnowledgeToolParamDocs.SEARCH_KNOWLEDGE_POINTS_ARGS_PARAM)
            SearchKnowledgePointArgs args) {
        try {
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }
            if (args == null || args.getQuery() == null || args.getQuery().isBlank()) {
                return new ToolResult<>(false, null, "检索参数 query 不能为空", null);
            }

            Integer topK = args.getTopK() == null || args.getTopK() <= 0
                    ? DEFAULT_TOP_K
                    : args.getTopK();
            Double minScore = args.getMinScore() == null
                    ? DEFAULT_MIN_SCORE
                    : args.getMinScore();

            List<KnowledgePointHitDTO> data = searchService.searchKnowledgePoints(
                    args.getKnowledgeScopes(),
                    args.getQuery(),
                    topK,
                    minScore
            );

            return new ToolResult<>(true, null, null, data);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        } catch (Exception e) {
            return new ToolResult<>(false, null, "知识点检索失败，请稍后重试", null);
        }
    }
    @Tool(KnowledgeToolDocs.SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS)
    public ToolResult<List<QuestionHitDTO>> searchQuestionsByKnowledgePoints(
            ChatSessionContext context,
            @P(KnowledgeToolParamDocs.SEARCH_QUESTIONS_BY_KNOWLEDGE_POINTS_ARGS_PARAM)
            SearchQuestionsByKnowledgePointsArgs args) {
        try {
            if (context == null) {
                return new ToolResult<>(false, null, "上下文已过期或不存在", null);
            }
            if (args == null || args.getKnowledgePointIds() == null || args.getKnowledgePointIds().isEmpty()) {
                return new ToolResult<>(false, null, "检索参数 knowledgePointIds 不能为空", null);
            }

            List<Long> collectionIds = context.getCollectionIds();
            if (collectionIds == null || collectionIds.isEmpty()) {
                return new ToolResult<>(false, null, "当前未绑定题集，无法按知识点检索题目", null);
            }

            List<QuestionHitDTO> data = searchService.searchQuestionsByKnowledgePoints(
                    args.getKnowledgePointIds(),
                    collectionIds,
                    args.getTopK(),
                    args.getTypeCode(),
                    args.getDifficultyMin(),
                    args.getDifficultyMax()
            );
            return new ToolResult<>(true, null, null, data);
        } catch (ApplicationException e) {
            return new ToolResult<>(false, e.getCode(), e.getMessage(), null);
        } catch (Exception e) {
            return new ToolResult<>(false, null, "按知识点检索题目失败，请稍后重试", null);
        }
    }
}
