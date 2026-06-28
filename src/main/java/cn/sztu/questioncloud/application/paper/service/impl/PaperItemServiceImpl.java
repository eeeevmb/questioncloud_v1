package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperItemService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.ExposureFactorUtil;
import cn.sztu.questioncloud.common.util.PaperWeightAlgorithmUtil;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperItemSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.req.RandomBuildReq;
import cn.sztu.questioncloud.web.rest.v1.paper.req.RandomReplaceReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperItemServiceImpl implements PaperItemService {

    private final QuestionCollectionRepository questionCollectionRepository;
    private final PaperRepository paperRepository;
    private final PaperItemRepository paperItemRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final QuestionVersionRepository questionVersionRepository;

    // === 人工组卷 ===
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PaperItemVO> savePaperItems(Long paperId, List<PaperItemSaveReq> reqs){
        this.deleteItemsByPaperId(paperId);
        if (reqs == null || reqs.isEmpty()) {
            return new ArrayList<>();
        }
        return this.addPaperItems(paperId, reqs);
    }

    @Override
    public List<PaperItemVO> addPaperItems(Long paperId, List<PaperItemSaveReq> reqs) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2 校验存在性与权限
        validatePaperStatus(paperEntity, userId);
        validateQuestionVersions(reqs);

        // 3. 获取属性
        Integer maxSeq = paperEntity.getTotalItems() == null ? 0 : paperEntity.getTotalItems();
        BigDecimal totalScore = paperEntity.getTotalScore() == null ? BigDecimal.ZERO : paperEntity.getTotalScore();
        List<PaperItemEntity> newEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 4. 构建新增试卷题目实体
        for (PaperItemSaveReq req : reqs) {
            totalScore = totalScore.add(req.getScore());
            maxSeq++;

            PaperItemEntity entity = PaperItemEntity.builder()
                    .paperId(paperId)
                    .seq(maxSeq)
                    .questionId(req.getQuestionId())
                    .questionVersionId(req.getQuestionVersionId())
                    .score(req.getScore())
                    .createdAt(now)
                    .build();
            newEntities.add(entity);
        }
        paperItemRepository.saveBatchItems(newEntities);

        PaperEntity statsUpdate = PaperEntity.builder()
                .id(paperId)
                .totalItems(maxSeq)
                .totalScore(totalScore)
                .updatedAt(now)
                .build();

        paperRepository.updateStatistics(statsUpdate);

        // 5. 构建返回结果
        List<PaperItemVO> result = new ArrayList<>();
        for (PaperItemEntity entity : newEntities) {
            PaperItemVO vo = PaperItemVO.builder()
                    .questionId(entity.getQuestionId())
                    .questionVersionId(entity.getQuestionVersionId())
                    .build();
            result.add(vo);
        }

        return result;
    }

    @Override
    public void deleteItemsByPaperId(Long paperId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2. 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        // 3. 执行删除
        paperItemRepository.deleteItemsByPaperId(paperId);
        LocalDateTime now = LocalDateTime.now();
        PaperEntity statsUpdate = PaperEntity.builder()
                .id(paperId)
                .totalItems(0)
                .totalScore(BigDecimal.ZERO)
                .updatedAt(now)
                .build();

        paperRepository.updateStatistics(statsUpdate);
    }

    @Override
    public List<PaperItemDetailVO> getItemsByPaperId(Long paperId) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2. 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        // 3. 查询并返回结果
        return paperItemRepository.getDetailedItemsByPaperId(paperId);
    }

    // === 随机组卷 ===
    @Override
    public List<PaperItemDetailVO> previewRandomItems(RandomBuildReq req) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 权限校验
        for (Long collectionId : req.getCollectionIds()) {
            QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在"));
            if (!userId.equals(collectionEntity.getOwnerId())) {
                throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "无权限访问该题集");
            }
        }

        List<PaperItemDetailVO> finalItems = new ArrayList<>();

        // 2. 循环处理每种题型的抽取规则
        for (RandomBuildReq.Rule rule : req.getRules()) {
            if (!QuestionTypeEnum.ensureValid(rule.getTypeCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR);
            }
            Double targetDiff = rule.getExpectedDifficulty();
            List<QuestionDetailVO> candidates = questionQueryRepository.findIdsByCollectionsAndType(
                    req.getCollectionIds(), rule.getTypeCode());

            if (candidates.size() < rule.getCount()) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_QUANTITY_INSUFFICIENT,
                        String.format("题库余量不足！[%s]需要 %d 道，题集中实际只有 %d 道",
                                rule.getTypeCode(), rule.getCount(), candidates.size()));
            }

            // 基于曝光度加权抽取
            List<QuestionDetailVO> selected = candidates.stream()
                    .map(vo -> {
                        double u = ThreadLocalRandom.current().nextDouble();
                        if (u <= 0) u = 1e-10;
                        double effectiveExp = ExposureFactorUtil.calcEffectiveExposure(
                                vo.getExposureFactor(), vo.getLastExposedAt(), LocalDateTime.now());
                        double weight = PaperWeightAlgorithmUtil.calcExponentialWeight(
                                effectiveExp, vo.getDifficulty(), targetDiff);
                        double key = PaperWeightAlgorithmUtil.calcGumbelKey(weight, u);
                        return new AbstractMap.SimpleEntry<>(key, vo);
                    })
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .limit(rule.getCount())
                    .sorted(Comparator.comparingDouble(vo ->
                            vo.getDifficulty() == null ? 0.0 : vo.getDifficulty()
                    ))
                    .toList();

           for (QuestionDetailVO vo : selected) {
               PaperItemDetailVO itemVO = PaperItemDetailVO.builder()
                       .questionId(vo.getId())
                        .questionVersionId(vo.getCurrentVersionId())
                       .questionTitle(vo.getTitle())
                        .stem(vo.getStem())
                        .typeCode(vo.getTypeCode())
                        .difficulty(vo.getDifficulty())
                        .correctRate(vo.getCorrectRate())
                        .options(vo.getOptions())
                        .assets(vo.getAssets())
                        .versionNo(vo.getVersionNo())
                        .build();

                finalItems.add(itemVO);
            }
        }

        return finalItems;
    }

    @Override
    public PaperItemDetailVO randomReplaceItem(RandomReplaceReq req) {
        Long userId = StpUtil.getLoginIdAsLong();

        // 1. 权限校验
        for (Long collectionId : req.getCollectionIds()) {
            QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "题集不存在"));
            if (!userId.equals(collectionEntity.getOwnerId())) {
                throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND, "无权限访问该题集");
            }
        }

        if (!QuestionTypeEnum.ensureValid(req.getTypeCode())) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR);
        }
        Double targetDiff = req.getExpectedDifficulty();
        // 2. 获取候选并过滤
        List<QuestionDetailVO> candidates = questionQueryRepository.findIdsByCollectionsAndType(
                req.getCollectionIds(), req.getTypeCode());

        List<Long> excludedIds = req.getExcludedQuestionIds();
        if (excludedIds != null && !excludedIds.isEmpty()) {
            Set<Long> excludeSet = new HashSet<>(excludedIds);
            candidates = candidates.stream()
                    .filter(vo -> !excludeSet.contains(vo.getId()))
                    .toList();
        }

        if (candidates.isEmpty()) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_QUANTITY_INSUFFICIENT, "无符合条件的题目可替换");
        }

        // 3. 加权抽取单题
        QuestionDetailVO selected = candidates.stream()
                .map(p -> {
                    double ran = ThreadLocalRandom.current().nextDouble();
                    if (ran <= 0) ran = 1e-10;
                    double effectiveExp = ExposureFactorUtil.calcEffectiveExposure(
                            p.getExposureFactor(), p.getLastExposedAt(), LocalDateTime.now());
                    double weight = PaperWeightAlgorithmUtil.calcExponentialWeight(
                            effectiveExp, p.getDifficulty(), targetDiff);
                    double key = PaperWeightAlgorithmUtil.calcGumbelKey(weight, ran);
                    return new AbstractMap.SimpleEntry<>(key, p);
                })
                .min(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.QUESTION_QUANTITY_INSUFFICIENT, "抽题失败"));

        return PaperItemDetailVO.builder()
               .questionId(selected.getId())
                .questionVersionId(selected.getCurrentVersionId())
               .questionTitle(selected.getTitle())
                .stem(selected.getStem())
                .typeCode(selected.getTypeCode())
                .difficulty(selected.getDifficulty())
                .correctRate(selected.getCorrectRate())
                .versionNo(selected.getVersionNo())
                .options(selected.getOptions())
                .assets(selected.getAssets())
                .build();
    }

    // === 其他 ===
    /**
     * 校验试卷状态与权限
     *
     * @param paperEntity 试卷实体
     * @param userId      当前操作用户ID
     */
    private void validatePaperStatus(PaperEntity paperEntity, Long userId) {
        // 1. 校验是否存在
        if (paperEntity == null) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_NOT_FOUND);
        }

        // 2. 校验权限 (TODO: 后续扩展多人协同)
        if (!userId.equals(paperEntity.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }
    }

    /**
     * 校验请求中的题目版本是否真实存在
     *
     * @param reqs 请求列表
     */
    private void validateQuestionVersions(List<PaperItemSaveReq> reqs) {
        if (reqs == null || reqs.isEmpty()) {
            return;
        }

        // 1. 提取去重后的 VersionID
        Set<Long> reqVersionIds = reqs.stream()
                .map(PaperItemSaveReq::getQuestionVersionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (reqVersionIds.isEmpty()) {
            return;
        }

        // 2. 批量查库
        List<Long> existingIds = questionVersionRepository.findExistingIds(reqVersionIds);

        // 3. 比对数量
        if (existingIds.size() != reqVersionIds.size()) {
            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND,
                    "部分题目版本不存在或已被删除，请刷新题库后重试");
        }
    }
}
