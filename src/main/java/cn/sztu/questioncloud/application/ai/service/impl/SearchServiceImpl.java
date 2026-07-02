package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.KnowledgePointHitDTO;
import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.port.QuestionSearchPort;
import cn.sztu.questioncloud.application.ai.service.SearchService;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgeQuestionSearchDTO;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.RAGSearchParam;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SearchServiceImpl implements SearchService {

    private final QuestionSearchPort questionSearchPort;
    private final KnowledgePointVectorService knowledgePointVectorService;
    private final KnowledgePointService knowledgePointService;

    public SearchServiceImpl(QuestionSearchPort questionSearchPort,
                             KnowledgePointVectorService knowledgePointVectorService,
                             KnowledgePointService knowledgePointService) {
        this.questionSearchPort = questionSearchPort;
        this.knowledgePointVectorService = knowledgePointVectorService;
        this.knowledgePointService = knowledgePointService;
    }

    @Override
    public List<QuestionHitDTO> searchQuestions(Long userId,
                                                List<Long> collectionIds,
                                                String query,
                                                RAGSearchParam ragSearchParam) {
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

    @Override
    public List<KnowledgePointHitDTO> searchKnowledgePoints(List<String> knowledgeScopes,
                                                            String query,
                                                            Integer topK,
                                                            Double minScore) {
        List<KnowledgePointVectorSearchVO> candidates = knowledgePointVectorService.searchCandidates(
                knowledgeScopes, query, topK, minScore);

        AtomicInteger similarityRank = new AtomicInteger(1);
        return candidates.stream()
                .map(item -> KnowledgePointHitDTO.builder()
                        .knowledgePointId(item.getKnowledgePointId())
                        .canonicalName(item.getCanonicalName())
                        .scopeNames(parseScopeNames(item.getSubject()))
                        .similarityRank(similarityRank.getAndIncrement())
                        .build())
                .toList();
    }

    @Override
    public List<QuestionHitDTO> searchQuestionsByKnowledgePoints(List<Long> knowledgePointIds,
                                                                 List<Long> collectionIds,
                                                                 Integer topK,
                                                                 String typeCode,
                                                                 Double difficultyMin,
                                                                 Double difficultyMax) {
        List<KnowledgeQuestionSearchDTO> candidates = knowledgePointService.searchQuestionsByKnowledgePoints(
                knowledgePointIds,
                collectionIds,
                topK,
                typeCode,
                difficultyMin,
                difficultyMax
        );

        AtomicInteger similarityRank = new AtomicInteger(1);
        return candidates.stream()
                .map(item -> QuestionHitDTO.builder()
                        .similarityRank(similarityRank.getAndIncrement())
                        .title(item.getTitle())
                        .stemPreview(item.getStemPreview())
                        .typeCode(item.getTypeCode())
                        .difficulty(item.getDifficulty())
                        .questionId(item.getQuestionId())
                        .questionVersionId(item.getQuestionVersionId())
                        .exposureFactor(item.getExposureFactor())
                        .fromCollectionName(item.getFromCollectionName())
                        .build())
                .toList();
    }

    private List<String> parseScopeNames(String subject) {
        if (subject == null || subject.isBlank()) {
            return List.of();
        }
        return List.of(subject.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }
}
