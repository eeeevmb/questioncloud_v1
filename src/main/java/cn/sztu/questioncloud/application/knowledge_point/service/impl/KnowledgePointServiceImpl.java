package cn.sztu.questioncloud.application.knowledge_point.service.impl;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSourceTypeEnum;
import cn.sztu.questioncloud.application.knowledge_point.enums.KnowledgeSubjectEnum;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgePointRepository;
import cn.sztu.questioncloud.application.knowledge_point.port.KnowledgeQuestionRelRepository;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointService;
import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgePointVectorService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KnowledgePointServiceImpl implements KnowledgePointService{
    private final KnowledgePointRepository knowledgePointRepository;
    private final KnowledgeQuestionRelRepository knowledgeQuestionRelRepository;
    private final KnowledgePointVectorService knowledgePointVectorService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<KnowledgeQuestionRelEntity> bindExtractedKnowledgePoints(Long questionId, Long questionVersionId, List<KnowledgePointExtractDTO> extractedPoints) {
        if (extractedPoints == null || extractedPoints.isEmpty()) {
            return List.of();
        }
        List<KnowledgeQuestionRelEntity> newEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for(KnowledgePointExtractDTO dto : extractedPoints) {
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
        knowledgeQuestionRelRepository.deleteRelationsByQuestionId(questionVersionId);
        knowledgeQuestionRelRepository.batchSave(newEntities);
        return newEntities;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KnowledgePointEntity findOrCreateKnowledgePoint(KnowledgePointExtractDTO dto) {
        // 过滤 + 格式处理
        String subject = Optional.ofNullable(dto.getSubject())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(KnowledgeSubjectEnum.OTHER.getDescription());
        String name = Optional.ofNullable(dto.getCanonicalName())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new ApplicationException(
                        CommonResultCodeEnum.PARAM_ERROR,
                        "知识点名称不能为空"
                ));
        // candidates大概率只有一个元素，但仍做处理
        List<KnowledgePointEntity> candidates =
                knowledgePointRepository.listByCanonicalNameOrAlias(subject, name);
        Optional<KnowledgePointEntity> matched = candidates.stream()
                .filter(candidate -> {
                    String canonicalName = Optional.ofNullable(candidate.getCanonicalName())
                            .map(String::trim)
                            .orElse("");

                    boolean canonicalMatched = canonicalName.equals(name);

                    boolean aliasMatched = Optional.ofNullable(candidate.getAliases())
                            .orElse(List.of())
                            .stream()
                            .filter(alias -> alias != null && !alias.isBlank())
                            .map(String::trim)
                            .anyMatch(name::equals);

                    return canonicalMatched || aliasMatched;
                })
                .findFirst();

        if (matched.isPresent()) {
            return matched.get();
        }

        LocalDateTime now = LocalDateTime.now();

        KnowledgePointEntity entity = KnowledgePointEntity.builder()
                .subject(subject)
                .canonicalName(name)
                .sourceType(KnowledgeSourceTypeEnum.AI_EXTRACTED.getCode())
                .isDeleted(0)
                .createdAt(now)
                .updatedAt(now)
                .build();

        knowledgePointRepository.save(entity);
        knowledgePointVectorService.upsert(entity);

        return entity;
    }

}
