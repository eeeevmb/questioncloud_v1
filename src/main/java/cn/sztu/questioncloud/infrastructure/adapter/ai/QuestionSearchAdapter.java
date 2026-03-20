package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.QuestionHitDTO;
import cn.sztu.questioncloud.application.ai.port.QuestionSearchPort;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.question.port.CollectionItemRepository;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class QuestionSearchAdapter implements QuestionSearchPort {
    private final VectorPort vectorPort;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionStatRepository questionStatRepository;
    private final QuestionQueryRepository questionQueryRepository;

    public static final Double DEFAULT_MIN_SCORE = 0.3D;
    public static final Integer DEFAULT_MAX_RESULTS = 10;

    @Override
    public List<QuestionHitDTO> searchQuestions(String query, SearchFilter filter) {
        EmbeddingSearchResult<TextSegment> searchResult = vectorPort.search(SearchQuery.builder()
                        .query(query)
                        .minScore(DEFAULT_MIN_SCORE)
                        .maxResult(DEFAULT_MAX_RESULTS)
                        .filter(filter)
                        .build());

        List<Long> questionIds = searchResult.matches().stream()
                // 按照RAG检索得分排序
                .sorted(Comparator.comparingDouble((EmbeddingMatch<TextSegment> m) -> m.score()).reversed())
                // 删去Q_前缀
                .map((match) -> Long.parseLong(match.embeddingId().replaceFirst("^Q_", "")))
                .toList();


        Map<Long, QuestionCollectionEntity> collectionEntityMap = questionQueryRepository.getQuestionIdToCollectionMap(questionIds);
        Map<Long, QuestionVersionEntity> versionEntityMap = questionVersionRepository.getCurrentVersionsByQuestionIds(questionIds);
        Map<Long, QuestionStat> questionStatMap = questionStatRepository.getByQuestionIds(questionIds);


        List<QuestionHitDTO> result = new ArrayList<>();
        for (Long questionId : questionIds) {
            String stem = versionEntityMap.get(questionId).getStem();

            result.add(QuestionHitDTO.builder()
                    .matchRank(result.size() + 1)
                    .stemPreview(stem.substring(0, Math.min(stem.length(), 200)))   // 截取前200个字符
                    .typeCode(versionEntityMap.get(questionId).getTypeCode())
                    .difficulty(questionStatMap.get(questionId).getDifficulty())
                    .fromCollectionName(collectionEntityMap.get(questionId).getName())
                    .build());
        }
        return result;
    }
}
