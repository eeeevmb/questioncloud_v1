package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.application.common.enums.VectorDocTypeEnum;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointScopeRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.config.ChromaProperties;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO.KnowledgePointVectorOverviewItemVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO.KnowledgePointVectorOverviewVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorDetailVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private final KnowledgePointScopeRelRepository knowledgePointScopeRelRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final ChromaProperties chromaProperties;
    private final ObjectMapper objectMapper;

    @Override
    public void upsert(KnowledgePointEntity entity) {
        String vectorId = VECTOR_ID_PREFIX + entity.getId();
        List<String> scopeNames = listScopeNamesByKnowledgePointId(entity);
        String primarySubject = scopeNames.isEmpty() ? null : scopeNames.getFirst();
        String scopeNamesText = joinScopeNames(scopeNames);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("docType", VectorDocTypeEnum.KNOWLEDGE_POINT.getCode());
        metadata.put("knowledgePointId", entity.getId());
        metadata.put("subject", primarySubject);
        metadata.put("scopeNames", scopeNamesText);
        metadata.put("canonicalName", entity.getCanonicalName());
        metadata.put("createdAt", formatDateTime(entity.getCreatedAt()));
        metadata.put("updatedAt", formatDateTime(entity.getUpdatedAt()));

        vectorPort.upsert(vectorId, buildVectorText(entity, scopeNamesText), metadata);
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
        int requestedTopK = topK == null ? DEFAULT_TOP_K : topK;
        int actualTopK = isBlank(subject) ? requestedTopK : Math.max(requestedTopK * 5, requestedTopK);

        SearchFilter filter = SearchFilter.builder()
                .docType(VectorDocTypeEnum.KNOWLEDGE_POINT.getCode())
                .build();

        EmbeddingSearchResult<TextSegment> result = vectorPort.search(SearchQuery.builder()
                .query(query)
                .maxResult(actualTopK)
                .minScore(minScore == null ? DEFAULT_MIN_SCORE : minScore)
                .filter(filter)
                .build());

        return result.matches().stream()
                .filter(match -> match.embeddingId() != null)
                .filter(match -> match.embeddingId().startsWith(VECTOR_ID_PREFIX))
                .filter(match -> metadataMatchesSubject(match.embedded(), subject))
                .map(match -> {
                    String vectorId = match.embeddingId();
                    TextSegment segment = match.embedded();
                    return KnowledgePointVectorSearchVO.builder()
                            .vectorId(vectorId)
                            .knowledgePointId(Long.parseLong(vectorId.substring(VECTOR_ID_PREFIX.length())))
                            .score(match.score())
                            .subject(readDisplaySubject(segment))
                            .canonicalName(segment.metadata().getString("canonicalName"))
                            .text(segment.text())
                            .build();
                })
                .limit(requestedTopK)
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
                .subject(asString(metadata.getOrDefault("scopeNames", metadata.get("subject"))))
                .canonicalName(asString(metadata.get("canonicalName")))
                .document(document)
                .metadata(metadata)
                .build();
    }

    @Override
    public int clearKnowledgePointVectors() {
        List<KnowledgePointVectorOverviewItemVO> items = listKnowledgePointVectors();
        for (KnowledgePointVectorOverviewItemVO item : items) {
            vectorPort.delete(item.getVectorId());
        }
        return items.size();
    }

    @Override
    public KnowledgePointVectorOverviewVO getKnowledgePointVectorOverview() {
        List<KnowledgePointVectorOverviewItemVO> allItems = listKnowledgePointVectors();
        List<KnowledgePointVectorOverviewItemVO> latestTenItems = allItems.stream()
                .sorted(Comparator.comparing(this::parseUpdatedAtSafely).reversed())
                .limit(10)
                .toList();

        return KnowledgePointVectorOverviewVO.builder()
                .totalCount(allItems.size())
                .latestTenItems(latestTenItems)
                .build();
    }

    private String buildVectorText(KnowledgePointEntity entity, String scopeNamesText) {
        return "科目：\n" + nullToEmpty(scopeNamesText)
                + "\n\n知识点：\n" + nullToEmpty(entity.getCanonicalName())
                + "\n\n描述：\n" + nullToEmpty(entity.getDescription())
                + "\n\n别名：\n" + (entity.getAliases() == null ? "" : String.join("、", entity.getAliases()))
                + "\n\n公式或代码：\n" + nullToEmpty(entity.getFormulaOrCode())
                + "\n\n示例：\n" + nullToEmpty(entity.getExample());
    }

    private List<String> listScopeNamesByKnowledgePointId(KnowledgePointEntity entity) {
        if (entity == null || entity.getId() == null) {
            return List.of();
        }

        List<String> scopeNames = knowledgePointScopeRelRepository.listByKnowledgePointId(entity.getId()).stream()
                .map(KnowledgePointScopeRelEntity::getKnowledgeScopeId)
                .map(knowledgeScopeRepository::getById)
                .filter(Objects::nonNull)
                .map(KnowledgeScopeEntity::getScopeName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isBlank())
                .distinct()
                .toList();

        return scopeNames.isEmpty() ? List.of() : scopeNames;
    }

    private boolean metadataMatchesSubject(TextSegment segment, String subject) {
        if (isBlank(subject)) {
            return true;
        }
        String normalizedSubject = subject.trim();
        String scopeNames = segment.metadata().getString("scopeNames");
        if (!isBlank(scopeNames)) {
            for (String item : scopeNames.split(",")) {
                if (normalizedSubject.equals(item.trim())) {
                    return true;
                }
            }
        }
        String primarySubject = segment.metadata().getString("subject");
        return normalizedSubject.equals(primarySubject);
    }

    private String readDisplaySubject(TextSegment segment) {
        String scopeNames = segment.metadata().getString("scopeNames");
        return isBlank(scopeNames) ? segment.metadata().getString("subject") : scopeNames;
    }

    private String joinScopeNames(List<String> scopeNames) {
        if (scopeNames == null || scopeNames.isEmpty()) {
            return "";
        }
        return String.join(",", scopeNames);
    }

    private String resolveCollectionId() {
        String responseBody = HttpRequest.get(buildCollectionListEndpoint())
                .execute()
                .body();

        JsonNode root = readJson(responseBody);
        JsonNode collectionsNode = root.isArray() ? root : root.path("data");
        if (!collectionsNode.isArray()) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "Chroma返回的collection列表格式异常");
        }

        for (JsonNode collectionNode : collectionsNode) {
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

    private List<KnowledgePointVectorOverviewItemVO> listKnowledgePointVectors() {
        String collectionId = resolveCollectionId();
        String responseBody = HttpRequest.post(buildGetEndpoint(collectionId))
                .contentType(ContentType.JSON.toString())
                .body(buildGetAllBody())
                .execute()
                .body();

        JsonNode root = readJson(responseBody);
        JsonNode idsNode = root.path("ids");
        JsonNode documentsNode = root.path("documents");
        JsonNode metadatasNode = root.path("metadatas");

        if (!idsNode.isArray()) {
            throw new ApplicationException(CommonResultCodeEnum.ERROR, "Chroma返回的向量列表格式异常");
        }

        List<KnowledgePointVectorOverviewItemVO> items = new ArrayList<>();
        for (int i = 0; i < idsNode.size(); i++) {
            String vectorId = idsNode.get(i).asText(null);
            if (vectorId == null || !vectorId.startsWith(VECTOR_ID_PREFIX)) {
                continue;
            }

            Map<String, Object> metadata = readMetadata(metadatasNode, i);
            items.add(KnowledgePointVectorOverviewItemVO.builder()
                    .vectorId(vectorId)
                    .knowledgePointId(parseKnowledgePointId(vectorId))
                    .subject(asString(metadata.getOrDefault("scopeNames", metadata.get("subject"))))
                    .canonicalName(asString(metadata.get("canonicalName")))
                    .createdAt(asString(metadata.get("createdAt")))
                    .updatedAt(asString(metadata.get("updatedAt")))
                    .document(readArrayText(documentsNode, i))
                    .metadata(metadata)
                    .build());
        }
        return items;
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

    private String buildGetAllBody() {
        Map<String, Object> payload = new HashMap<>();
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

    private Long parseKnowledgePointId(String vectorId) {
        try {
            return Long.parseLong(vectorId.substring(VECTOR_ID_PREFIX.length()));
        } catch (Exception e) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? null : time.toString();
    }

    private LocalDateTime parseUpdatedAtSafely(KnowledgePointVectorOverviewItemVO item) {
        if (item == null || isBlank(item.getUpdatedAt())) {
            return LocalDateTime.MIN;
        }
        try {
            return LocalDateTime.parse(item.getUpdatedAt());
        } catch (DateTimeParseException e) {
            return LocalDateTime.MIN;
        }
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
