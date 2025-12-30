package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperItemService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.application.question.port.QuestionVersionRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperItemSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemSaveVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperItemServiceImpl implements PaperItemService {

    private final PaperRepository paperRepository;
    private final PaperItemRepository paperItemRepository;
    private final QuestionVersionRepository questionVersionRepository;

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
            // 进阶优化：找出具体哪个ID不存在，方便排查
            // reqVersionIds.removeAll(existingIds);
            // log.warn("检测到不存在的题目版本ID: {}", reqVersionIds);

            throw new ApplicationException(QuestionErrorCodeEnum.QUESTION_NOT_FOUND,
                    "部分题目版本不存在或已被删除，请刷新题库后重试");
        }
    }
}
