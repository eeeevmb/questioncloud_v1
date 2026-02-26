package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.application.paper.port.PaperPresenceCheckerRepository;
import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperAppService;
import cn.sztu.questioncloud.application.question.enums.QuestionErrorCodeEnum;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.*;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAppServiceImpl implements PaperAppService {
    private final PaperRepository paperRepository;
    private final PaperItemRepository paperItemRepository;
    private final PaperQueryRepository paperQueryRepository;
    private final PaperPresenceCheckerRepository paperPresenceCheckerRepository;
    public static final Integer INITIAL_COUNT = 0;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperCreatedVO createPaper(PaperSaveReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 业务校验
        boolean exists = paperPresenceCheckerRepository.existsByTitle(req.getTitle(), userId);
        if (exists) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_TITLE_DUPLICATE, "您已存在同名试卷，请勿重复创建");
        }

        // 3. 创建空试卷实体
        Long paperId = HutoolSnowflakeIdGenerator.generateLongId();
        LocalDateTime now = LocalDateTime.now();

        PaperEntity paperEntity = PaperEntity.builder()
                .id(paperId)
                .ownerId(userId)
                .title(req.getTitle())
                .description(req.getDescription())
                .totalItems(INITIAL_COUNT)
                .totalScore(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .build();

        paperRepository.save(paperEntity);

        // 4. 返回试卷创建视图
        return PaperCreatedVO.builder()
                .paperId(paperId)
                .build();
    }

    @Override
    public PageResult<PaperBasicVO> searchPapers(PaperPageQuery query) {
        Long userId = StpUtil.getLoginIdAsLong();
        Pager<PaperBasicVO> paging = paperQueryRepository.searchPapers(query, userId);
        return PageResult.of(paging.getResults(), paging.getTotal(), query);
    }

    @Override
    public PaperDetailVO getPaperDetailById(Long paperId) {
        PaperEntity paperEntity = paperRepository.getById(paperId);
        Long userId = StpUtil.getLoginIdAsLong();
        validatePaperStatus(paperEntity, userId);

        PaperDetailVO result = BeanUtil.toBean(paperEntity, PaperDetailVO.class);
        List<PaperItemDetailVO> items = paperQueryRepository.listPaperItemsByPaperId(paperId);
        result.setItems(items);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePaperById(Long paperId) {
        // 1. 基础查询
        PaperEntity paperEntity = paperRepository.getById(paperId);
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        // 3. 删除试卷及其关联题目
        paperRepository.delete(paperId);
        paperItemRepository.deleteItemsByPaperId(paperId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperBasicVO updatePaperInfo(Long paperId, PaperSaveReq req) {
        // 1. 基础查询
        PaperEntity paperEntity = paperRepository.getById(paperId);
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        if (!req.getTitle().equals(paperEntity.getTitle())) {
            boolean exists = paperPresenceCheckerRepository.existsByTitle(req.getTitle(), userId);
            if (exists) {
                throw new ApplicationException(PaperErrorCodeEnum.PAPER_TITLE_DUPLICATE, "您已存在同名试卷，请使用其他名称");
            }
            paperEntity.setTitle(req.getTitle());
        }

        // 3. 更新 title以外的字段
        LocalDateTime now = LocalDateTime.now();
        paperEntity.setDescription(req.getDescription());
        paperEntity.setUpdatedAt(now);

        paperRepository.updateStatistics(paperEntity);
        paperEntity = paperRepository.getById(paperId);

        // 4. 返回结果
        return PaperBasicVO.builder()
                .id(paperEntity.getId())
                .title(paperEntity.getTitle())
                .description(paperEntity.getDescription())
                .totalItems(paperEntity.getTotalItems())
                .totalScore(paperEntity.getTotalScore())
                .updatedAt(now)
                .build();
    }

    // === 其他 ===
    /**
     * 校验试卷状态与权限
     */
    private void validatePaperStatus(PaperEntity paperEntity, Long userId) {
        // 1. 校验是否存在
        if (paperEntity == null) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_NOT_FOUND);
        }

        // 2. 校验权限
        if (!userId.equals(paperEntity.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }
    }
}
