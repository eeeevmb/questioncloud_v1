package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.port.QuestionSearchPort;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {

    private final QuestionSearchPort questionSearchPort;

    public SearchServiceImpl(QuestionSearchPort questionSearchPort) {
        this.questionSearchPort = questionSearchPort;
    }

    @Override
    public List<QuestionHitDTO> searchQuestions(Long userId, List<Long> collectionIds, String query, RAGSearchParam ragSearchParam) {
        RAGSearchParam safeParam = ragSearchParam == null ? RAGSearchParam.builder().build() : ragSearchParam;
        SearchFilter filter = SearchFilter.builder()
                .docType(VectorDocTypeEnum.QUESTION.getCode())
                .ownerId(userId)
                .collectionIds(collectionIds)
                .difficultyMin(safeParam.getDifficultyMin())
                .difficultyMax(safeParam.getDifficultyMax())
                .typeCode(safeParam.getTypeCode())
                .build();

        return questionSearchPort.searchQuestions(query, filter);
    }
}
