package cn.sztu.questioncloud.infrastructure.common.ai.service;

import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeExtractorPrompts;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

/**
 * 知识点提取 AI 服务
 *
 * <p>基于 LangChain4j 的 {@link AiService} 声明式接口，
 * 用于从题目中提取知识点。</p>
 *
 * <p>不需要会话记忆和工具调用，是单次文本处理任务。</p>
 */
@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        chatModel = "openAiChatModel"
)
public interface KnowledgeExtractorAiService {
    /**
     * 从题目文本中确定知识点领域
     *
     * @param questionText 题目完整文本（包含题干、选项、答案、解析）
     * @return 提取的知识点列表
     */
    @SystemMessage(KnowledgeExtractorPrompts.ROUTE_QUESTION_KNOWLEDGE_SCOPE)
    String routeQuestionKnowledgeScope(@UserMessage String questionText);

    /**
     * 从题目文本中提取知识点
     *
     * @param questionText 题目完整文本（包含题干、选项、答案、解析）
     * @return 提取的知识点列表
     */
    @SystemMessage(KnowledgeExtractorPrompts.EXTRACT_KNOWLEDGE_POINT_TAGS)
    String extractKnowledgePointsFromQuestion(@UserMessage String questionText);

    /**
     * 从目录文本中提取知识点
     *
     * @param directoryText 目录文本（包含章节标题、层级等信息）
     * @return 提取的知识点列表
     */
    @SystemMessage(KnowledgeExtractorPrompts.EXTRACT_KNOWLEDGE_POINTS_FROM_DIRECTORY)
    String extractKnowledgePointsFromDirectory(@UserMessage String directoryText);

    /**
     * 从题目文本中提取知识点详情
     *
     * @param knowledgePointText 知识点文本（通常是知识点名称或简要描述）
     * @return 知识点详情文本（包含定义、相关概念、应用场景等）
     */
    @SystemMessage(KnowledgeExtractorPrompts.ENRICH_KNOWLEDGE_POINT_DETAIL)
    String enrichKnowledgePointDetail(@UserMessage String knowledgePointText);
}