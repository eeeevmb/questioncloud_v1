package cn.sztu.questioncloud.application.ai.service.impl;

import cn.dev33.satoken.stp.StpUtil;
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


    /**
     * RAG妫€绱㈤闆嗗唴鐩稿叧棰樼洰
     * @param userId         鐢ㄦ埛ID
     * @param collectionId   棰橀泦ID
     * @param query          鐢ㄦ埛鐨勮嚜鐒惰瑷€鎻忚堪
     * @param ragSearchParam 绛涢€夋潯浠?
     * @return 鍛戒腑鐨勯鐩垪琛?
     */
    @Override
    public List<QuestionHitDTO> RAGSearch(Long userId, Long collectionId, String query, RAGSearchParam ragSearchParam) {
        // 鏋勫缓RAG妫€绱㈣姹?
        SearchFilter filter = SearchFilter.builder()
                .docType(VectorDocTypeEnum.QUESTION.getCode())
                .ownerId(userId)
                .collectionId(collectionId)
                .difficultyMin(ragSearchParam.getDifficultyMin())
                .difficultyMax(ragSearchParam.getDifficultyMax())
                .typeCode(ragSearchParam.getTypeCode())
                .build();

        return questionSearchPort.searchQuestions(query, filter);
    }
}
