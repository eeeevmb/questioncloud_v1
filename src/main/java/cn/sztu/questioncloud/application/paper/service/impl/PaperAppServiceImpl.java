package cn.sztu.questioncloud.application.paper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.application.paper.port.PaperCheckRepository;
import cn.sztu.questioncloud.application.paper.port.PaperQueryRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.application.paper.service.PaperAppService;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.req.PaperSaveReq;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperBasicVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperCreatedVO;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperAppServiceImpl implements PaperAppService {
    private final PaperRepository paperRepository;
    private final PaperQueryRepository paperQueryRepository;
    private final PaperCheckRepository paperCheckRepository;
    public static final Integer INITIAL_COUNT = 0;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperCreatedVO createPaper(PaperSaveReq req) {
        // 1. 获取用户ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 业务校验
        boolean exists = paperCheckRepository.existsByTitle(req.getTitle(), userId);
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
        // 1. 权限、存在性校验
        Long userId = StpUtil.getLoginIdAsLong();
        paperCheckRepository.getAndValidate(paperId, userId, PaperStatusEnum.DRAFT);

        // 2. 删除试卷及其关联题目
        paperRepository.delete(paperId);

        // 2.x 删除关联的试题
        // paperItemRepository.deleteByPaperId(paperId);
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
        // 1. 权限、存在性校验
        Long userId = StpUtil.getLoginIdAsLong();
        PaperEntity entity = paperCheckRepository.getAndValidate(paperId, userId, PaperStatusEnum.DRAFT);

        // 2. 更新字段
        entity.setTitle(req.getTitle());
        entity.setDescription(req.getDescription());
        entity.setUpdatedAt(LocalDateTime.now());

        paperRepository.update(entity);
        LocalDateTime now = LocalDateTime.now();

        // 3. 返回结果
        return PaperBasicVO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .status(entity.getStatus())
                .totalItems(entity.getTotalItems())
                .totalScore(entity.getTotalScore())
                .updatedAt(now)
                .build();
    }
}
