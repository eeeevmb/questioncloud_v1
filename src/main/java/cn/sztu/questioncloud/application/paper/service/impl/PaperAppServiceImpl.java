package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.application.paper.port.PaperItemRepository;
import cn.sztu.questioncloud.application.paper.port.PaperPresenceCheckerRepository;
import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperAppService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.model.vo.PageResult;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperPageQuery;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperListItemVO;
import cn.xbatis.core.mybatis.mapper.context.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        //TODO:允许创建同名试卷，创建时在试卷末端填补"(N-1)"
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
                .status(PaperStatusEnum.DRAFT.getCode())
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

    /**
     * 根据试卷ID查询题目详情
     *
     * @param paperId 试卷ID
     * @return 试卷详情视图
     */
    @Override
    public PaperDetailVO getPaperDetailById(Long paperId){
        Long userId = StpUtil.getLoginIdAsLong();
        Optional<PaperDetailVO> detailVO = paperQueryRepository.getPaperDetailById(paperId);

        // 1. 校验结果与验证
        PaperDetailVO result = detailVO
                .orElseThrow(() -> new ApplicationException(PaperErrorCodeEnum.PAPER_NOT_FOUND));
        if (!result.getOwnerId().equals(userId)) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        List<PaperItemVO> items = paperItemRepository.getDetailedItemsByPaperId(paperId);
        result.setItems(items);

        // 2. 返回结果
        return result;
    }

    /**
     * 硬删除 paper、paperItem
     *
     * @param paperId 试卷ID
     */
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

    /**
     * 修改草稿试卷信息(重命名、修改描述)
     *
     * @param paperId 试卷ID
     * @return 试卷详情视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperBasicVO updatePaperInfo(Long paperId, PaperSaveReq req){
        // 1. 基础查询
        PaperEntity paperEntity = paperRepository.getById(paperId);
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 校验存在性与权限
        validatePaperStatus(paperEntity, userId);

        boolean exists = paperPresenceCheckerRepository.existsByTitle(req.getTitle(), userId);
        if (exists) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_TITLE_DUPLICATE, "您已存在同名试卷，请勿重复创建");
        }

        // 3. 更新字段
        paperEntity.setTitle(req.getTitle());
        paperEntity.setDescription(req.getDescription());
        paperEntity.setUpdatedAt(LocalDateTime.now());

        paperRepository.update(paperEntity);
        LocalDateTime now = LocalDateTime.now();

        // 4. 返回结果
        return PaperBasicVO.builder()
                .id(paperEntity.getId())
                .title(paperEntity.getTitle())
                .description(paperEntity.getDescription())
                .status(paperEntity.getStatus())
                .totalItems(paperEntity.getTotalItems())
                .totalScore(paperEntity.getTotalScore())
                .updatedAt(now)
                .build();
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

    @Override
    public PageResult<PaperListItemVO> pagePapers(PaperPageQuery query) {
        if (query == null) {
            query = new PaperPageQuery();
        }
        query.valid();
        Long userId = StpUtil.getLoginIdAsLong();
        Pager<PaperListItemVO> paging = paperQueryRepository.pageByOwnerId(userId, query);
        return PageResult.of(paging.getResults(), paging.getTotal(), query);
    }
}
