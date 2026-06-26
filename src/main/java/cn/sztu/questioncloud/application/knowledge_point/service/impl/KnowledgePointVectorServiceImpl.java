package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.config.ChromaProperties;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorDetailVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
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
    private static final String CHROMA_TENANT = "default";
    private static final String CHROMA_DATABASE = "default";

    private final VectorPort vectorPort;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final ChromaProperties chromaProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void upsert(KnowledgePointEntity entity) {
        String vectorId = VECTOR_ID_PREFIX + entity.getId();
        String subject = getSubjectByKnowledgeScopeId(entity.getKnowledgeScopeId());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", VectorDocTypeEnum.KNOWLEDGE_POINT.getCode());
        metadata.put("knowledgePointId", entity.getId());
        metadata.put("subject", subject);
        metadata.put("canonicalName", entity.getCanonicalName());

        vectorPort.upsert(vectorId, buildVectorText(entity), metadata);
    }

    @Override
    public void delete(Long knowledgePointId) {
        vectorPort.delete(VECTOR_ID_PREFIX + knowledgePointId);
    }

    @Override
    public int reindexAll() {
        List<KnowledgePointEntity> entities = knowledgePointRepository.listAllActive();
        for (KnowledgePointEntity entity : entities) {
            upsert(entity);
        }
        return entities.size();
    }

    @Override
    public List<KnowledgePointVectorSearchVO> searchCandidates(String subject, String query, Integer topK, Double minScore) {
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
                .filter(match -> match.embeddingId() != null)
                .filter(match -> match.embeddingId().startsWith(VECTOR_ID_PREFIX))
                .map(match -> {
                    String vectorId = match.embeddingId();
                    TextSegment segment = match.embedded();
                    return KnowledgePointVectorSearchVO.builder()
                            .vectorId(vectorId)
                            .knowledgePointId(Long.parseLong(vectorId.substring(VECTOR_ID_PREFIX.length())))
                            .score(match.score())
                            .subject(segment.metadata().getString("subject"))
                            .canonicalName(segment.metadata().getString("canonicalName"))
                            .text(segment.text())
                            .build();
                })
                .toList();
    }

    @Override
    public KnowledgePointVectorDetailVO getVectorDetail(Long knowledgePointId) {
        if (knowledgePointId == null) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "knowledgePointId不能为空");
        }

        String vectorId = VECTOR_ID_PREFIX + knowledgePointId;
        String collectionId = resolveCollectionId();
        String responseBody = HttpRequest.post(buildGetEndpoint(collectionId))
                .contentType(ContentType.JSON.toString())
                .body(buildGetBody(vectorId))
                .execute()
                .body();

        JsonNode root = readJson(responseBody);
        JsonNode idsNode = root.path("ids");
        if (!idsNode.isArray() || idsNode.isEmpty()) {
            throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "向量不存在: " + vectorId);
        }

        int targetIndex = findVectorIndex(idsNode, vectorId);
        if (targetIndex < 0) {
            throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND, "向量不存在: " + vectorId);
        }

        String document = readArrayText(root.path("documents"), targetIndex);
        Map<String, Object> metadata = readMetadata(root.path("metadatas"), targetIndex);

        return KnowledgePointVectorDetailVO.builder()
                .knowledgePointId(knowledgePointId)
                .vectorId(vectorId)
                .collectionId(collectionId)
                .collectionName(chromaProperties.getCollection())
                .subject(asString(metadata.get("subject")))
                .canonicalName(asString(metadata.get("canonicalName")))
                .document(document)
                .metadata(metadata)
                .build();
    }

    private String buildVectorText(KnowledgePointEntity entity) {
        return "科目：\n" + nullToEmpty(getSubjectByKnowledgeScopeId(entity.getKnowledgeScopeId()))
                + "\n\n知识点：\n" + nullToEmpty(entity.getCanonicalName())
                + "\n\n描述：\n" + nullToEmpty(entity.getDescription())
                + "\n\n别名：\n" + (entity.getAliases() == null ? "" : String.join("、", entity.getAliases()))
                + "\n\n公式或代码：\n" + nullToEmpty(entity.getFormulaOrCode())
                + "\n\n示例：\n" + nullToEmpty(entity.getExample());
    }

    private String getSubjectByKnowledgeScopeId(Long knowledgeScopeId) {
        if (knowledgeScopeId == null) {
            return null;
        }
        KnowledgeScopeEntity knowledgeScope = knowledgeScopeRepository.getById(knowledgeScopeId);
        return knowledgeScope == null ? null : knowledgeScope.getScopeName();
    }

    private String resolveCollectionId() {
        String responseBody = HttpRequest.get(buildCollectionListEndpoint())
                .execute()
                .body();

        JsonNode root = readJson(responseBody);
        JsonNode dataNode = root.path("data");
        if (!dataNode.isArray()) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "Chroma返回的collection列表格式异常");
        }

        for (JsonNode collectionNode : dataNode) {
            if (chromaProperties.getCollection().equals(collectionNode.path("name").asText())) {
                String collectionId = collectionNode.path("id").asText(null);
                if (collectionId != null && !collectionId.isBlank()) {
                    return collectionId;
                }
            }
        }

        throw new ApplicationException(CommonResultCodeEnum.NOT_FOUND,
                "Chroma中不存在collection: " + chromaProperties.getCollection());
    }

    private String buildCollectionListEndpoint() {
        return trimTrailingSlash(chromaProperties.getBaseUrl())
                + "/api/v2/tenants/" + CHROMA_TENANT
                + "/databases/" + CHROMA_DATABASE
                + "/collections";
    }

    private String buildGetEndpoint(String collectionId) {
        return trimTrailingSlash(chromaProperties.getBaseUrl())
                + "/api/v2/tenants/" + CHROMA_TENANT
                + "/databases/" + CHROMA_DATABASE
                + "/collections/" + collectionId
                + "/get";
    }

    private String buildGetBody(String vectorId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("ids", List.of(vectorId));
        payload.put("include", List.of("documents", "metadatas"));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "构造Chroma请求体失败");
        }
    }

    private JsonNode readJson(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException e) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "解析Chroma响应失败: " + responseBody);
        }
    }

    private int findVectorIndex(JsonNode idsNode, String vectorId) {
        for (int i = 0; i < idsNode.size(); i++) {
            if (vectorId.equals(idsNode.get(i).asText())) {
                return i;
            }
        }
        return -1;
    }

    private String readArrayText(JsonNode arrayNode, int index) {
        if (!arrayNode.isArray() || arrayNode.size() <= index || arrayNode.get(index).isNull()) {
            return null;
        }
        return arrayNode.get(index).asText();
    }

    private Map<String, Object> readMetadata(JsonNode metadataArrayNode, int index) {
        if (!metadataArrayNode.isArray() || metadataArrayNode.size() <= index || metadataArrayNode.get(index).isNull()) {
            return Map.of();
        }

        JsonNode metadataNode = metadataArrayNode.get(index);
        return objectMapper.convertValue(
                metadataNode,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class)
        );
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "未配置Chroma baseUrl");
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
