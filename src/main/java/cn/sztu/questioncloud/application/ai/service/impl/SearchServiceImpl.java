package cn.sztu.questioncloud.application.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.port.QuestionSearchPort;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    private final QuestionSearchPort questionSearchPort;

    public SearchServiceImpl(QuestionSearchPort questionSearchPort) {
        this.questionSearchPort = questionSearchPort;
    }


    /**
     * RAG检索题集内相关题目
     * @param userId         用户ID
     * @param collectionId   题集ID
     * @param query          用户的自然语言描述
     * @param ragSearchParam 筛选条件
     * @return 命中的题目列表
     */
    @Override
    public List<QuestionHitDTO> RAGSearch(Long userId, Long collectionId, String query, RAGSearchParam ragSearchParam) {
        // 构建RAG检索请求
        SearchFilter filter = SearchFilter.builder()
                .ownerId(userId)
                .collectionId(collectionId)
                .difficultyMin(ragSearchParam.getDifficultyMin())
                .difficultyMax(ragSearchParam.getDifficultyMax())
                .typeCode(ragSearchParam.getTypeCode())
                .build();

        return questionSearchPort.searchQuestions(query, filter);
    }
}
