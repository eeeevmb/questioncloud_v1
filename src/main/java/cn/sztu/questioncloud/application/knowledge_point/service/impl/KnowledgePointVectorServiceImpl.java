package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgePointVectorServiceImpl implements KnowledgePointVectorService {
    private static final String VECTOR_ID_PREFIX = "KP_";
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.5D;

    private final VectorPort vectorPort;

    @Override
    public void upsert(KnowledgePointEntity entity) {
        String vectorId = VECTOR_ID_PREFIX + entity.getId();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", VectorDocTypeEnum.KNOWLEDGE_POINT.getCode());
        metadata.put("knowledgePointId", entity.getId());
        metadata.put("subject", entity.getSubject());
        metadata.put("canonicalName", entity.getCanonicalName());

        vectorPort.upsert(vectorId, buildVectorText(entity), metadata);
    }

    @Override
    public void delete(Long knowledgePointId) {
        vectorPort.delete(VECTOR_ID_PREFIX + knowledgePointId);
    }

    @Override
    public List<Long> searchCandidateIds(String subject, String query, Integer topK, Double minScore) {
        SearchFilter filter = SearchFilter.builder()
                .docType(VectorDocTypeEnum.KNOWLEDGE_POINT.getCode())
                .subject(subject)
                .build();

        EmbeddingSearchResult<TextSegment> result = vectorPort.search(SearchQuery.builder()
                .query(query)
                .maxResult(topK == null ? DEFAULT_TOP_K : topK)
                .minScore(minScore == null ? DEFAULT_MIN_SCORE : minScore)
                .filter(filter)
                .build());

        return result.matches().stream()
                .map(EmbeddingMatch::embeddingId)
                .filter(id -> id != null && id.startsWith(VECTOR_ID_PREFIX))
                .map(id -> Long.parseLong(id.substring(VECTOR_ID_PREFIX.length())))
                .toList();
    }

    private String buildVectorText(KnowledgePointEntity entity) {
        return "科目：\n" + nullToEmpty(entity.getSubject()) +
                "\n\n知识点：\n" + nullToEmpty(entity.getCanonicalName()) +
                "\n\n描述：\n" + nullToEmpty(entity.getDescription()) +
                "\n\n别名：\n" + (entity.getAliases() == null ? "" : String.join("、", entity.getAliases())) +
                "\n\n公式或代码：\n" + nullToEmpty(entity.getFormulaOrCode()) +
                "\n\n示例：\n" + nullToEmpty(entity.getExample());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}