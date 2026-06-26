package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.DirectoryKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointDetailDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
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
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.QuestionOption;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class KnowledgeExtractServiceImpl implements KnowledgeExtractService {

    private final AiJsonParser aiJsonParser;
    private final CollectionItemRepository collectionItemRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final KnowledgeExtractorAiService knowledgeExtractorAiService;
    private final KnowledgePointService knowledgePointService;
    private final KnowledgePointVectorService knowledgePointVectorService;

    @Override
    public List<DirectoryKnowledgeExtractDTO> extractFromDirectoryText(String directoryText) {
        String extractedPointsText =
                knowledgeExtractorAiService.extractKnowledgePointsFromDirectory(directoryText);

        List<DirectoryKnowledgeExtractDTO> extractedPoints =
                parseDirectoryExtractResult(extractedPointsText);

        if (extractedPoints.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI目录知识点提取结果为空，原始返回: " + extractedPointsText
            );
        }
        return extractedPoints;
    }

    @Override
    public List<QuestionKnowledgeExtractDTO> extractAndBind(Long questionVersionId) {
        QuestionVersionEntity questionVersion = questionVersionRepository.getVersionById(questionVersionId);
        if (questionVersion == null) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND, "题目版本不存在");
        }

        QuestionVersionEntity currentVersion =
                questionVersionRepository.getCurrentVersionByQuestionId(questionVersion.getQuestionId());
        if (currentVersion == null || !Objects.equals(currentVersion.getId(), questionVersion.getId())) {
            return List.of();
        }

        String questionText = buildQuestionText(questionVersion);
        String extractedPointsText = knowledgeExtractorAiService.extractKnowledgePointsFromQuestion(questionText);
        List<QuestionKnowledgeExtractDTO> extractedPoints = parseExtractResult(extractedPointsText);
        if (extractedPoints.isEmpty()) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "AI没有提取到知识点，原始返回: " + extractedPointsText
            );
        }

        knowledgePointService.bindExtractedKnowledgePoints(
                questionVersion.getQuestionId(),
                questionVersion.getId(),
                extractedPoints
        );
        enrichExtractedKnowledgePoints(questionVersion.getId());
        return extractedPoints;
    }

    private void enrichExtractedKnowledgePoints(Long questionVersionId) {
        List<Long> knowledgePointIds = knowledgeQuestionRelRepository
                .listByQuestionVersionId(questionVersionId)
                .stream()
                .map(KnowledgeQuestionRelEntity::getKnowledgePointId)
                .toList();
        enrichMissingDetails(knowledgePointIds);
    }

    @Override
    public Map<Long, List<QuestionKnowledgeExtractDTO>> extractAndBindCollection(Long collectionId) {
        List<Long> questionVersionIds =
                collectionItemRepository.listVersionIdsByCollectionId(collectionId);

        if (questionVersionIds == null || questionVersionIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<QuestionKnowledgeExtractDTO>> result = new LinkedHashMap<>();

        for (Long questionVersionId : questionVersionIds) {
            List<QuestionKnowledgeExtractDTO> extractedPoints = extractAndBind(questionVersionId);
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
        appendLine(builder, "科目", getSubjectByKnowledgeScopeId(entity.getKnowledgeScopeId()));
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

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private boolean needEnrich(KnowledgePointEntity entity) {
        return isBlank(entity.getDescription())
                || isBlank(entity.getExample())
                || isBlank(entity.getFormulaOrCode());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String getSubjectByKnowledgeScopeId(Long knowledgeScopeId) {
        if (knowledgeScopeId == null) {
            return null;
        }
        KnowledgeScopeEntity knowledgeScope = knowledgeScopeRepository.getById(knowledgeScopeId);
        return knowledgeScope == null ? null : knowledgeScope.getScopeName();
    }

    private String buildQuestionText(QuestionVersionEntity version) {
        StringBuilder builder = new StringBuilder();

        appendLine(builder, "题型", version.getTypeCode());
        appendLine(builder, "标题", version.getTitle());
        appendLine(builder, "题干", version.getStem());

        if (version.getOptions() != null && !version.getOptions().isEmpty()) {
            builder.append("选项:\n");
            for (QuestionOption option : version.getOptions()) {
                builder.append(nullToEmpty(option.getKey()))
                        .append(". ")
                        .append(nullToEmpty(option.getContent()))
                        .append("\n");
            }
            builder.append("\n");
        }

        appendLine(builder, "答案", version.getAnswer());
        appendLine(builder, "判分答案", version.getAnswerKey());
        appendLine(builder, "解析", version.getSolution());

        return builder.toString();
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
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

    private KnowledgePointDetailDTO parseDetailResult(String text) {
        return aiJsonParser.parseObject(
                text,
                new TypeReference<KnowledgePointDetailDTO>() {
                },
                "AI知识点详情补全结果"
        );
    }
}
