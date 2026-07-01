package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgeQuestionSearchDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeTagDTO;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSourceTypeEnum;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSubjectEnum;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointScopeRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeScopeRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.application.question.port.CollectionItemRepository;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.application.question.port.QuestionStatRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService {

    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgePointScopeRelRepository knowledgePointScopeRelRepository;
    private final KnowledgeScopeRepository knowledgeScopeRepository;
    private final KnowledgePointVectorService knowledgePointVectorService;
    private final CollectionItemRepository collectionItemRepository;
    private final QuestionVersionRepository questionVersionRepository;
    private final QuestionStatRepository questionStatRepository;
    private final QuestionQueryRepository questionQueryRepository;

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
    public List<QuestionKnowledgeTagDTO> listQuestionKnowledgeTags(Long questionVersionId) {
        if (questionVersionId == null) {
            throw new ApplicationException(CommonResultCodeEnum.PARAM_ERROR, "题目版本ID不能为空");
        }

        List<KnowledgeQuestionRelEntity> relEntities =
                knowledgeQuestionRelRepository.listByQuestionVersionId(questionVersionId);
        if (relEntities == null || relEntities.isEmpty()) {
            return List.of();
        }

        return relEntities.stream()
                .map(relEntity -> {
                    KnowledgePointEntity entity = knowledgePointRepository.getById(relEntity.getKnowledgePointId());
                    if (entity == null) {
                        return null;
                    }
                    return QuestionKnowledgeTagDTO.builder()
                            .knowledgePointId(entity.getId())
                            .canonicalName(entity.getCanonicalName())
                            .isMain(relEntity.getIsMain())
                            .relevanceScore(relEntity.getRelevanceScore())
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(QuestionKnowledgeTagDTO::getIsMain, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QuestionKnowledgeTagDTO::getRelevanceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QuestionKnowledgeTagDTO::getKnowledgePointId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
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

    @Override
    public List<KnowledgeQuestionSearchDTO> searchQuestionsByKnowledgePoints(List<Long> knowledgePointIds,
                                                                             List<Long> collectionIds,
                                                                             Integer topK,
                                                                             String typeCode,
                                                                             Double difficultyMin,
                                                                             Double difficultyMax) {
        List<Long> distinctKnowledgePointIds = Optional.ofNullable(knowledgePointIds)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctKnowledgePointIds.isEmpty()) {
            return List.of();
        }

        List<Long> distinctCollectionIds = Optional.ofNullable(collectionIds)
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinctCollectionIds.isEmpty()) {
            return List.of();
        }

        List<Long> questionVersionIds = distinctCollectionIds.stream()
                .flatMap(collectionId -> collectionItemRepository.listVersionIdsByCollectionId(collectionId).stream())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (questionVersionIds.isEmpty()) {
            return List.of();
        }

        List<KnowledgeQuestionRelEntity> relEntities =
                knowledgeQuestionRelRepository.listByKnowledgePointIdsAndQuestionVersionIds(
                        distinctKnowledgePointIds,
                        questionVersionIds
                );
        if (relEntities.isEmpty()) {
            return List.of();
        }

        Map<Long, QuestionMatchAggregate> aggregateMap = new LinkedHashMap<>();
        for (KnowledgeQuestionRelEntity relEntity : relEntities) {
            if (relEntity.getQuestionId() == null) {
                continue;
            }
            QuestionMatchAggregate aggregate = aggregateMap.computeIfAbsent(
                    relEntity.getQuestionId(),
                    questionId -> new QuestionMatchAggregate(questionId, relEntity.getQuestionVersionId())
            );
            aggregate.addRelation(relEntity);
        }
        if (aggregateMap.isEmpty()) {
            return List.of();
        }

        List<Long> questionIds = aggregateMap.keySet().stream().toList();
        Map<Long, QuestionVersionEntity> versionEntityMap =
                questionVersionRepository.getCurrentVersionsByQuestionIds(questionIds);
        Map<Long, QuestionStat> questionStatMap = questionStatRepository.getByQuestionIds(questionIds);
        Map<Long, QuestionCollectionEntity> collectionEntityMap =
                questionQueryRepository.getQuestionIdToCollectionMap(questionIds);

        List<KnowledgeQuestionSearchDTO> result = aggregateMap.values().stream()
                .map(aggregate -> toSearchDTO(
                        aggregate,
                        versionEntityMap.get(aggregate.questionId),
                        questionStatMap.get(aggregate.questionId),
                        collectionEntityMap.get(aggregate.questionId)
                ))
                .filter(Objects::nonNull)
                .filter(dto -> typeCode == null || typeCode.isBlank() || typeCode.equals(dto.getTypeCode()))
                .filter(dto -> difficultyMin == null || (dto.getDifficulty() != null && dto.getDifficulty() >= difficultyMin))
                .filter(dto -> difficultyMax == null || (dto.getDifficulty() != null && dto.getDifficulty() <= difficultyMax))
                .sorted(Comparator
                        .comparing(KnowledgeQuestionSearchDTO::getMainKnowledgePointCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeQuestionSearchDTO::getMatchedKnowledgePointCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeQuestionSearchDTO::getBestRelevanceScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeQuestionSearchDTO::getExposureFactor, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int limit = topK == null || topK <= 0 ? 10 : topK;
        return result.stream()
                .limit(limit)
                .toList();
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

    private KnowledgeQuestionSearchDTO toSearchDTO(QuestionMatchAggregate aggregate,
                                                   QuestionVersionEntity versionEntity,
                                                   QuestionStat questionStat,
                                                   QuestionCollectionEntity collectionEntity) {
        if (aggregate == null || versionEntity == null) {
            return null;
        }

        String stem = Optional.ofNullable(versionEntity.getStem()).orElse("");
        return KnowledgeQuestionSearchDTO.builder()
                .questionId(aggregate.questionId)
                .questionVersionId(Optional.ofNullable(versionEntity.getId()).orElse(aggregate.questionVersionId))
                .title(versionEntity.getTitle())
                .stemPreview(stem.substring(0, Math.min(stem.length(), 200)))
                .typeCode(versionEntity.getTypeCode())
                .difficulty(questionStat == null ? null : questionStat.getDifficulty())
                .exposureFactor(questionStat == null ? null : questionStat.getExposureFactor())
                .fromCollectionName(collectionEntity == null ? null : collectionEntity.getName())
                .matchedKnowledgePointCount(aggregate.matchedKnowledgePointIds.size())
                .mainKnowledgePointCount(aggregate.mainKnowledgePointCount)
                .bestRelevanceScore(aggregate.bestRelevanceScore)
                .matchedKnowledgePointIds(List.copyOf(aggregate.matchedKnowledgePointIds))
                .build();
    }

    private static class QuestionMatchAggregate {
        private final Long questionId;
        private final Long questionVersionId;
        private final Set<Long> matchedKnowledgePointIds = new LinkedHashSet<>();
        private int mainKnowledgePointCount = 0;
        private int bestRelevanceScore = Integer.MIN_VALUE;

        private QuestionMatchAggregate(Long questionId, Long questionVersionId) {
            this.questionId = questionId;
            this.questionVersionId = questionVersionId;
        }

        private void addRelation(KnowledgeQuestionRelEntity relEntity) {
            if (relEntity.getKnowledgePointId() != null) {
                matchedKnowledgePointIds.add(relEntity.getKnowledgePointId());
            }
            if (Objects.equals(relEntity.getIsMain(), 1)) {
                mainKnowledgePointCount++;
            }
            if (relEntity.getRelevanceScore() != null) {
                bestRelevanceScore = Math.max(bestRelevanceScore, relEntity.getRelevanceScore());
            }
        }
    }
}
