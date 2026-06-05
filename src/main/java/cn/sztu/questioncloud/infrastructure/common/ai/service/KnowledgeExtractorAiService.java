package cn.sztu.questioncloud.infrastructure.common.ai.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;
import cn.sztu.questioncloud.infrastructure.common.ai.constant.KnowledgeExtractorPrompts;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

import java.util.List;

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
     * 从题目文本中提取知识点
     *
     * @param questionText 题目完整文本（包含题干、选项、答案、解析）
     * @return 提取的知识点列表
     */
    @SystemMessage(KnowledgeExtractorPrompts.EXTRACT_KNOWLEDGE_POINT_TAGS)
    String extractKnowledgePoints(@UserMessage String questionText);

    @SystemMessage(KnowledgeExtractorPrompts.ENRICH_KNOWLEDGE_POINT_DETAIL)
    String enrichKnowledgePointDetail(@UserMessage String knowledgePointText);
}