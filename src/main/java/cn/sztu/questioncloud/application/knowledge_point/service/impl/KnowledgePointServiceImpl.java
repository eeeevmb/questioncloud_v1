package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSourceTypeEnum;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSubjectEnum;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointScopeRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgePointScopeRelRepository knowledgePointScopeRelRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final KnowledgePointVectorService knowledgePointVectorService;

    @Override
    public void addKnowledgeScope(String scopeName) {
        if (scopeName == null || scopeName.isBlank()) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "知识点领域名称不能为空");
        }
        LocalDateTime now = LocalDateTime.now();
        KnowledgeScopeEntity newScope = KnowledgeScopeEntity.builder()
                .scopeName(scopeName.trim())
                .createdAt(now)
                .updatedAt(now)
                .isDeleted(0)
                .build();
        knowledgeScopeRepository.save(newScope);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void bindExtractedKnowledgePoints(Long questionId,
                                             Long questionVersionId,
                                             List<QuestionKnowledgeExtractDTO> extractedPoints) {
        if (questionId == null || questionVersionId == null) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "题目ID和题目版本ID不能为空");
        }

        knowledgeQuestionRelRepository.deleteByQuestionId(questionId);

        if (extractedPoints == null || extractedPoints.isEmpty()) {
            return;
        }

        List<KnowledgeQuestionRelEntity> newEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (QuestionKnowledgeExtractDTO dto : extractedPoints) {
            KnowledgePointEntity entity = findOrCreateKnowledgePoint(dto);
            KnowledgeQuestionRelEntity relEntity = KnowledgeQuestionRelEntity.builder()
                    .questionId(questionId)
                    .questionVersionId(questionVersionId)
                    .knowledgePointId(entity.getId())
                    .isMain(dto.getIsMain())
                    .relevanceScore(dto.getRelevanceScore())
                    .sourceType(KnowledgeSourceTypeEnum.AI_EXTRACTED.getCode())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            newEntities.add(relEntity);
        }
        knowledgeQuestionRelRepository.batchSave(newEntities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePointEntity findOrCreateKnowledgePoint(QuestionKnowledgeExtractDTO dto) {
        String knowledgeScope = Optional.ofNullable(dto.getKnowledgeScope())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(KnowledgeSubjectEnum.OTHER.getDescription());
        Long knowledgeScopeId = resolveKnowledgeScopeId(knowledgeScope);

        String name = Optional.ofNullable(dto.getCanonicalName())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new ApplicationException(
                        CommonResultCodeEnum.PARAM_ERROR,
                        "知识点名称不能为空"
                ));

        List<KnowledgePointEntity> candidates =
                Optional.ofNullable(knowledgePointRepository.listByCanonicalNameOrAlias(name))
                        .orElse(List.of());

        Optional<KnowledgePointEntity> matched = candidates.stream()
                .filter(candidate -> hasSameCanonicalOrAlias(candidate, name))
                .findFirst();

        LocalDateTime now = LocalDateTime.now();
        List<String> normalizedAliases = normalizeAliases(dto.getAliases(), name);
        if (matched.isPresent()) {
            KnowledgePointEntity entity = matched.get();
            List<String> mergedAliases = mergeAliases(entity.getAliases(), normalizedAliases, name);
            if (!Objects.equals(entity.getAliases(), mergedAliases)) {
                entity.setAliases(mergedAliases);
                knowledgePointRepository.update(entity);
            }
            bindKnowledgePointToScope(entity.getId(), knowledgeScopeId, now);
            knowledgePointVectorService.upsert(entity);
            return entity;
        }

        KnowledgePointEntity entity = KnowledgePointEntity.builder()
                .canonicalName(name)
                .aliases(normalizedAliases)
                .sourceType(KnowledgeSourceTypeEnum.AI_EXTRACTED.getCode())
                .createdAt(now)
                .updatedAt(now)
                .build();

        knowledgePointRepository.save(entity);
        bindKnowledgePointToScope(entity.getId(), knowledgeScopeId, now);
        knowledgePointVectorService.upsert(entity);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgePoints(List<Long> knowledgePointIds) {
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            return;
        }

        List<Long> distinctIds = knowledgePointIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        for (Long id : distinctIds) {
            KnowledgePointEntity entity = knowledgePointRepository.getById(id);
            if (entity == null) {
                continue;
            }
            knowledgeQuestionRelRepository.deleteByKnowledgePointId(id);
            knowledgePointScopeRelRepository.deleteByKnowledgePointId(id);
            knowledgePointRepository.delete(id);
            knowledgePointVectorService.delete(id);
        }
    }

    private boolean hasSameCanonicalOrAlias(KnowledgePointEntity entity, String normalizedName) {
        String canonicalName = Optional.ofNullable(entity.getCanonicalName())
                .map(String::trim)
                .orElse("");
        if (canonicalName.equals(normalizedName)) {
            return true;
        }
        return Optional.ofNullable(entity.getAliases())
                .orElse(List.of())
                .stream()
                .filter(alias -> alias != null && !alias.isBlank())
                .map(String::trim)
                .anyMatch(normalizedName::equals);
    }

    private List<String> normalizeAliases(List<String> aliases, String canonicalName) {
        if (aliases == null || aliases.isEmpty()) {
            return List.of();
        }

        String normalizedCanonicalName = Optional.ofNullable(canonicalName)
                .map(String::trim)
                .orElse("");

        return aliases.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(alias -> !alias.isBlank())
                .filter(alias -> !alias.equals(normalizedCanonicalName))
                .distinct()
                .toList();
    }

    private List<String> mergeAliases(List<String> existingAliases, List<String> incomingAliases, String canonicalName) {
        Set<String> merged = new LinkedHashSet<>();

        normalizeAliases(existingAliases, canonicalName).forEach(merged::add);
        normalizeAliases(incomingAliases, canonicalName).forEach(merged::add);

        return List.copyOf(merged);
    }

    private void bindKnowledgePointToScope(Long knowledgePointId, Long knowledgeScopeId, LocalDateTime now) {
        if (knowledgePointId == null || knowledgeScopeId == null) {
            return;
        }
        if (knowledgePointScopeRelRepository.exists(knowledgePointId, knowledgeScopeId)) {
            return;
        }
        knowledgePointScopeRelRepository.save(KnowledgePointScopeRelEntity.builder()
                .knowledgePointId(knowledgePointId)
                .knowledgeScopeId(knowledgeScopeId)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Long resolveKnowledgeScopeId(String knowledgeScope) {
        KnowledgeScopeEntity entity = knowledgeScopeRepository.getByScopeName(knowledgeScope);
        if (entity == null) {
            throw new ApplicationException(
                    CommonResultCodeEnum.PARAM_ERROR,
                    "知识点领域不存在: " + knowledgeScope
            );
        }
        return entity.getId();
    }
}
