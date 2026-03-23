package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;

import java.util.List;

/**
 * 检索服务
 */
public interface SearchService {
    /**
     * 自然语言检索题集内相关题目
     *
     * @param userId         用户ID
     * @param collectionIds  题集ID
     * @param query          用户的自然语言描述
     * @param ragSearchParam 筛选条件
     * @return 命中的题目列表
     */
    List<QuestionHitDTO> searchQuestions(Long userId, List<Long> collectionIds, String query, RAGSearchParam ragSearchParam);
}
