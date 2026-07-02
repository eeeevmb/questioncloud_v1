package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.ai.dto.KnowledgePointHitDTO;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;

import java.util.List;

/**
 * AI 检索服务
 */
public interface SearchService {

    /**
     * 自然语言检索题集内相关题目
     */
    List<QuestionHitDTO> searchQuestions(Long userId,
                                         List<Long> collectionIds,
                                         String query,
                                         RAGSearchParam ragSearchParam);

    /**
     * 检索相关知识点候选
     */
    List<KnowledgePointHitDTO> searchKnowledgePoints(List<String> knowledgeScopes,
                                                     String query,
                                                     Integer topK,
                                                     Double minScore);

    /**
     * 根据知识点列表检索当前题集内的关联题目
     */
    List<QuestionHitDTO> searchQuestionsByKnowledgePoints(List<Long> knowledgePointIds,
                                                          List<Long> collectionIds,
                                                          Integer topK,
                                                          String typeCode,
                                                          Double difficultyMin,
                                                          Double difficultyMax);
}
