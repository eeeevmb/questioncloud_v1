package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.DirectoryKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointDetailDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointScopeRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeExtractService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.port.CollectionItemRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.service.KnowledgeExtractorAiService;
import cn.sztu.questioncloud.infrastructure.common.ai.util.AiJsonParser;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.dto.QuestionOption;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgeExtractServiceImpl implements KnowledgeExtractService {
    private static final int MAX_EXISTING_KNOWLEDGE_POINT_CANDIDATES = 100;

    private final AiJsonParser aiJsonParser;
    private final CollectionItemRepository collectionItemRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgePointScopeRelRepository knowledgePointScopeRelRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final KnowledgeExtractorAiService knowledgeExtractorAiService;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointVectorService knowledgePointVectorService;

    @Override
    public List<DirectoryKnowledgeExtractDTO> extractFromDirectoryText(String directoryText) {
        String extractedPointsText = knowledgeExtractorAiService.extractKnowledgePointsFromDirectory(directoryText);
        List<DirectoryKnowledgeExtractDTO> extractedPoints = parseDirectoryExtractResult(extractedPointsText);

        if (extractedPoints.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI目录知识点提取结果为空，原始返回: " + extractedPointsText
            );
        }
        return extractedPoints;
    }

    @Override
    public List<QuestionKnowledgeExtractDTO> extractFromQuestionAndBind(Long questionVersionId) {
        QuestionVersionEntity questionVersion = questionVersionRepository.getVersionById(questionVersionId);
        if (questionVersion == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "题目版本不存在");
        }

        QuestionVersionEntity currentVersion =
                questionVersionRepository.getCurrentVersionByQuestionId(questionVersion.getQuestionId());
        if (currentVersion == null || !Objects.equals(currentVersion.getId(), questionVersion.getId())) {
            return List.of();
        }

        // 1. 先识别题目所属的知识点领域
        List<String> knowledgeDomains = identifyKnowledgeDomains(questionVersionId);
        List<String> existingKnowledgePointNames = listExistingKnowledgePointNamesByDomains(knowledgeDomains);

        // 2. 直接提取知识点，后续再由后端做查重、复用与保存
        String questionText = buildQuestionExtractText(
                questionVersion,
                knowledgeDomains,
                existingKnowledgePointNames
        );
        String extractedPointsText = knowledgeExtractorAiService.extractKnowledgePointsFromQuestion(questionText);
        List<QuestionKnowledgeExtractDTO> extractedPoints = parseExtractResult(extractedPointsText);
        if (extractedPoints.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI没有提取到知识点，原始返回: " + extractedPointsText
            );
        }

        String primaryKnowledgeScope = knowledgeDomains.isEmpty() ? "OTHER" : knowledgeDomains.get(0);
        extractedPoints.forEach(point -> {
            if (point.getKnowledgeScope() == null || point.getKnowledgeScope().isBlank()) {
                point.setKnowledgeScope(primaryKnowledgeScope);
            }
        });

        knowledgePointService.bindExtractedKnowledgePoints(
                questionVersion.getQuestionId(),
                questionVersion.getId(),
                extractedPoints
        );
        enrichExtractedKnowledgePoints(questionVersion.getId());
        return extractedPoints;
    }

    @Override
    public void enrichMissingDetails(List<Long> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }
        knowledgePointIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::enrichMissingDetails);
    }

    @Override
    public List<String> identifyKnowledgeDomains(Long questionVersionId) {
        QuestionVersionEntity entity = questionVersionRepository.getVersionById(questionVersionId);
        if (entity == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "题目版本不存在");
        }

        List<String> knowledgeScopes = knowledgeScopeRepository.getAllScopeNames();
        String identifyText = buildKnowledgeScopeRouteText(entity, knowledgeScopes);
        String routeResult = knowledgeExtractorAiService.routeQuestionKnowledgeScope(identifyText);
        return parseScopeRouteResult(routeResult);
    }

    @Override
    public Map<Long, List<QuestionKnowledgeExtractDTO>> extractAndBindCollection(Long collectionId) {
        List<Long> questionVersionIds = collectionItemRepository.listVersionIdsByCollectionId(collectionId);
        if (questionVersionIds == null || questionVersionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<QuestionKnowledgeExtractDTO>> result = new LinkedHashMap<>();
        for (Long questionVersionId : questionVersionIds) {
            List<QuestionKnowledgeExtractDTO> extractedPoints = extractFromQuestionAndBind(questionVersionId);
            result.put(questionVersionId, extractedPoints);
        }
        return result;
    }

    @Override
    public void enrichMissingDetails(Integer limit) {
        List<KnowledgePointEntity> entities = knowledgePointRepository.listNeedEnrich(limit);
        for (KnowledgePointEntity entity : entities) {
            enrichMissingDetails(entity.getId());
        }
    }

    private void enrichExtractedKnowledgePoints(Long questionVersionId) {
        List<Long> knowledgePointIds = knowledgeQuestionRelRepository
                .listByQuestionVersionId(questionVersionId)
                .stream()
                .map(KnowledgeQuestionRelEntity::getKnowledgePointId)
                .toList();
        enrichMissingDetails(knowledgePointIds);
    }

    private void enrichMissingDetails(Long knowledgePointId) {
        KnowledgePointEntity entity = knowledgePointRepository.getById(knowledgePointId);
        if (entity == null) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "知识点ID无效，无法补全: " + knowledgePointId
            );
        }

        if (!needEnrich(entity)) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        appendLine(builder, "科目", getScopeNamesByKnowledgePointId(entity));
        appendLine(builder, "知识点名称", entity.getCanonicalName());
        String knowledgeText = builder.toString();

        String knowledgeDetailsText = knowledgeExtractorAiService.enrichKnowledgePointDetail(knowledgeText);
        KnowledgePointDetailDTO detailDTO = parseDetailResult(knowledgeDetailsText);

        entity.setDescription(detailDTO.getDescription());
        entity.setExample(detailDTO.getExample());
        entity.setFormulaOrCode(detailDTO.getFormulaOrCode());
        knowledgePointRepository.update(entity);
        knowledgePointVectorService.upsert(entity);
    }

    private boolean needEnrich(KnowledgePointEntity entity) {
        return isBlank(entity.getDescription())
                || isBlank(entity.getExample())
                || isBlank(entity.getFormulaOrCode());
    }

    private List<QuestionKnowledgeExtractDTO> parseExtractResult(String text) {
        List<QuestionKnowledgeExtractDTO> result = aiJsonParser.parseArray(
                text,
                new TypeReference<List<QuestionKnowledgeExtractDTO>>() {
                },
                "AI知识点提取结果"
        );

        if (result == null || result.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI知识点提取结果为空数组"
            );
        }
        return result;
    }

    private List<DirectoryKnowledgeExtractDTO> parseDirectoryExtractResult(String text) {
        List<String> result = aiJsonParser.parseArray(
                text,
                new TypeReference<List<String>>() {
                },
                "AI目录知识点提取结果"
        );

        if (result == null || result.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI目录知识点提取结果为空数组"
            );
        }

        return result.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .map(name -> DirectoryKnowledgeExtractDTO.builder()
                        .canonicalName(name)
                        .build())
                .toList();
    }

    private KnowledgePointDetailDTO parseDetailResult(String text) {
        return aiJsonParser.parseObject(
                text,
                new TypeReference<KnowledgePointDetailDTO>() {
                },
                "AI知识点详情补全结果"
        );
    }

    private List<String> parseScopeRouteResult(String text) {
        List<String> result = aiJsonParser.parseArray(
                text,
                new TypeReference<List<String>>() {
                },
                "AI知识点领域判断结果"
        );

        List<String> normalized = result.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI知识点领域判断结果为空数组"
            );
        }
        return normalized;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getScopeNamesByKnowledgePointId(KnowledgePointEntity entity) {
        if (entity == null || entity.getId() == null) {
            return null;
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

        return scopeNames.isEmpty() ? null : String.join(",", scopeNames);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label)
                .append(":\n")
                .append(value)
                .append("\n\n");
    }

    private void appendStringListSection(StringBuilder builder, String label, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        List<String> normalizedValues = values.stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalizedValues.isEmpty()) {
            return;
        }

        builder.append(label).append(":\n");
        for (String value : normalizedValues) {
            builder.append("- ").append(value).append("\n");
        }
        builder.append("\n");
    }

    private boolean titleHasMeaning(String title) {
        if (title == null) {
            return false;
        }

        String normalized = title.trim();
        if (normalized.isBlank()) {
            return false;
        }

        return !normalized.matches("^第\\d+题$")
                && !normalized.matches("^题目\\d+$")
                && !normalized.matches("^测试题$")
                && !normalized.matches("^练习题$")
                && !normalized.matches("^例题$")
                && !normalized.matches("^习题$");
    }

    private boolean solutionHasMeaning(String solution) {
        return solution != null && !solution.isBlank();
    }

    private List<String> normalizeScopeNames(List<String> scopeNames) {
        Set<String> result = new LinkedHashSet<>();

        if (scopeNames != null) {
            for (String scopeName : scopeNames) {
                String normalized = trimToNull(scopeName);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        }

        result.add("OTHER");
        return List.copyOf(result);
    }

    private void appendOptionsSection(StringBuilder builder, List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return;
        }

        builder.append("选项:\n");
        for (QuestionOption option : options) {
            if (option == null) {
                continue;
            }

            String key = nullToEmpty(option.getKey()).trim();
            String content = nullToEmpty(option.getContent()).trim();
            if (key.isBlank() && content.isBlank()) {
                continue;
            }

            if (!key.isBlank()) {
                builder.append(key).append(". ");
            }
            builder.append(content).append("\n");
        }
        builder.append("\n");
    }

    // === 文本组装工具 ===

    private String buildQuestionText(QuestionVersionEntity version) {
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "题型", version.getTypeCode());
        if (titleHasMeaning(version.getTitle())) {
            appendLine(builder, "标题", version.getTitle());
        }
        appendLine(builder, "题干", version.getStem());
        appendOptionsSection(builder, version.getOptions());
        appendLine(builder, "答案", version.getAnswer());
        appendLine(builder, "判分答案", version.getAnswerKey());
        if (solutionHasMeaning(version.getSolution())) {
            appendLine(builder, "解析", version.getSolution());
        }

        return builder.toString();
    }

    private List<String> listExistingKnowledgePointNamesByDomains(List<String> knowledgeDomains) {
        List<Long> knowledgeScopeIds = Optional.ofNullable(knowledgeDomains)
                .orElse(List.of())
                .stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .map(knowledgeScopeRepository::getByScopeName)
                .filter(Objects::nonNull)
                .map(KnowledgeScopeEntity::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (knowledgeScopeIds.isEmpty()) {
            return List.of();
        }

        return knowledgePointScopeRelRepository.listKnowledgePointNamesByKnowledgeScopeIds(knowledgeScopeIds)
                .stream()
                .map(this::trimToNull)
                .filter(Objects::nonNull)
                .distinct()
                .limit(MAX_EXISTING_KNOWLEDGE_POINT_CANDIDATES)
                .toList();
    }

    private String buildQuestionExtractText(QuestionVersionEntity version,
                                            List<String> knowledgeDomains,
                                            List<String> existingKnowledgePointNames) {
        StringBuilder builder = new StringBuilder();

        appendStringListSection(builder, "已识别知识点领域", knowledgeDomains);
        appendStringListSection(builder, "当前领域已有知识点候选", existingKnowledgePointNames);
        builder.append("下面是题目内容:\n\n");
        builder.append(buildQuestionText(version));

        return builder.toString();
    }

    private String buildKnowledgeScopeRouteText(QuestionVersionEntity entity, List<String> scopeNames) {
        if (entity == null) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "题目版本不能为空");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("下面是一道题目的结构化信息，请根据题目核心考查内容判断它最适合归属到哪些知识点领域。")
                .append("\n\n");

        builder.append("题目内容:\n");
        appendLine(builder, "题型", entity.getTypeCode());
        if (titleHasMeaning(entity.getTitle())) {
            appendLine(builder, "标题", entity.getTitle());
        }
        appendLine(builder, "题干", entity.getStem());
        appendOptionsSection(builder, entity.getOptions());
        if (solutionHasMeaning(entity.getSolution())) {
            appendLine(builder, "解析", entity.getSolution());
        }

        builder.append("当前可选知识点领域:\n");
        for (String scopeName : normalizeScopeNames(scopeNames)) {
            builder.append("- ").append(scopeName).append("\n");
        }

        return builder.toString();
    }
}
