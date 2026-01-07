package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperItemService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.enums.QuestionTypeEnum;
import cn.sztu.questioncloud.application.question.port.CollectionPresenceCheckerPort;
import cn.sztu.questioncloud.application.question.port.QuestionCollectionRepository;
import cn.sztu.questioncloud.application.question.port.QuestionQueryRepository;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.ExposureFactorUtil;
import cn.sztu.questioncloud.common.util.PaperWeightAlgorithmUtil;
import cn.sztu.questioncloud.infrastructure.adapter.question.CollectionPresenceCheckerAdapter;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperItemSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.req.RandomBuildReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemSaveVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDetailVO;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionSummaryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperItemServiceImpl implements PaperItemService {

    private final QuestionCollectionRepository questionCollectionRepository;
    private final PaperRepository paperRepository;
    private final PaperItemRepository paperItemRepository;
    private final QuestionQueryRepository questionQueryRepository;
    private final QuestionVersionRepository questionVersionRepository;

    // === 自由组卷 ===

    @Override
    public List<PaperItemSaveVO> savePaperItems(Long paperId,List<PaperItemSaveReq> reqs){
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2 校验存在性与权限
        validatePaperStatus(paperEntity, userId);
        validateQuestionVersions(reqs);

        // 3. 数据预处理
        List<PaperItemEntity> newEntities = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalScore = BigDecimal.ZERO; // 试卷总分累加器

        // 4. 构建试卷题目实体并计算总分
        for (int i = 0; i < reqs.size(); i++) {
            PaperItemSaveReq req = reqs.get(i);
            PaperItemEntity entity = PaperItemEntity.builder()
                    .paperId(paperId)
                    .seq(i + 1) // 题序从1开始
                    .questionId(req.getQuestionId())
                    .questionVersionId(req.getQuestionVersionId())
                    .score(req.getScore())
                    .createdAt(now)
                    .build();
            newEntities.add(entity);
            totalScore = totalScore.add(req.getScore());
        }
        paperItemRepository.deleteItemsByPaperId(paperId);
        paperItemRepository.saveBatchItems(newEntities);

        PaperEntity statsUpdate = PaperEntity.builder()
                .id(paperId)
                .totalItems(newEntities.size())
                .totalScore(totalScore)
                .updatedAt(now)
                .build();

        paperRepository.updateStatistics(statsUpdate);

        // 5. 构建返回结果
        List<PaperItemSaveVO> result = new ArrayList<>();
        for (PaperItemEntity entity : newEntities) {
            PaperItemSaveVO vo = PaperItemSaveVO.builder()
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
    }

    @Override
    public List<PaperItemVO> getItemsByPaperId(Long paperId) {
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
    public List<PaperItemSaveVO> previewRandomItems(Long paperId, RandomBuildReq req){
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        for (Long collectionId : req.getCollectionIds()) {
            QuestionCollectionEntity collectionEntity = questionCollectionRepository.findById(collectionId)
                    .orElseThrow(() -> new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND));
            if (!userId.equals(collectionEntity.getOwnerId())) {
                throw new ApplicationException(QuestionErrorCodeEnum.COLLECTION_NOT_FOUND);
            }
        }

        // 3. 随机组卷逻辑
        List<PaperItemEntity> finalItems = new ArrayList<>();
        int currentSeq = 1; // 题号计数器

        for (RandomBuildReq.Rule rule : req.getRules()) {
            // 3.1 校验题目类型
            if (!QuestionTypeEnum.ensureValid(rule.getTypeCode())) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_TYPE_ERROR);
            }
            // 3.2 搜索所有符合条件的候选题目
            List<QuestionDetailVO> candidates = questionQueryRepository.findIdsByCollectionsAndType(
                    req.getCollectionIds(), rule.getTypeCode());
            // 3.3 校验库存
            if (candidates.size() < rule.getCount()) {
                throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_QUANTITY_INSUFFICIENT,
                        String.format("题库余量不足！[%s]需要 %d 道，题集中实际只有 %d 道",
                                rule.getTypeCode(), rule.getCount(), candidates.size()));
            }
            /*
            3.4 洗牌抽取
            Collections.shuffle(candidates, ThreadLocalRandom.current());
            List<QuestionSummaryVO> selected = new ArrayList<>(candidates.subList(0, rule.getCount()));
            */

            // 3.4 基于曝光度的加权抽取
            List<QuestionDetailVO> selected = candidates.stream()
                    .map(vo -> {
                        double u = ThreadLocalRandom.current().nextDouble();
                        if (u <= 0) u = 1e-10;

                        double effectiveExp = ExposureFactorUtil.calcEffectiveExposure(
                                vo.getExposureFactor(), vo.getLastExposedAt(), LocalDateTime.now());
                        double weight = PaperWeightAlgorithmUtil.calcExponentialWeight(effectiveExp);
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

            // 3.5 构建 PaperItem 实体
            for (QuestionDetailVO vo : selected) {
                PaperItemEntity item = PaperItemEntity.builder()
                        .paperId(paperId)
                        .questionId(vo.getId())
                        .questionVersionId(vo.getCurrentVersionId())
                        .score(rule.getScore())
                        .seq(currentSeq++)
                        .createdAt(LocalDateTime.now())
                        .build();

                finalItems.add(item);
            }
        }

        // 4. 返回抽取到的题目
        return finalItems.stream().map(item -> PaperItemSaveVO.builder()
                        .questionId(item.getQuestionId())
                        .questionVersionId(item.getQuestionVersionId())
                        .build())
                .collect(Collectors.toList());
    }

    // === 校验方法 ===

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

        // 3. 校验状态 (必须是草稿)
        if (!Objects.equals(paperEntity.getStatus(), PaperStatusEnum.DRAFT.getCode())) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_STATUS_ERROR,
                    "非草稿试卷无法修改试卷，当前状态：" + paperEntity.getStatus());
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
