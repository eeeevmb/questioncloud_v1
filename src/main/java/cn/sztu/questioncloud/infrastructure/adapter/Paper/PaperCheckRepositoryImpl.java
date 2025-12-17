package cn.sztu.questioncloud.infrastructure.adapter.Paper;

import cn.sztu.questioncloud.application.paper.enums.PaperErrorCodeEnum;
import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.application.paper.port.PaperCheckRepository;
import cn.sztu.questioncloud.application.paper.port.PaperRepository;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.mapper.paper.PaperMapper;
import cn.xbatis.core.sql.executor.chain.QueryChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Objects;

@Repository
@RequiredArgsConstructor
public class PaperCheckRepositoryImpl implements PaperCheckRepository {

    private final PaperMapper paperMapper;
    private final PaperRepository paperRepository;

    /**
     * 根据ID检查试卷是否存在
     *
     * @param title 试卷ID
     * @return 是否存在
     */
    @Override
    public boolean existsByTitle(String title, Long userId) {
        Integer count = QueryChain.of(paperMapper)
                .eq(PaperEntity::getTitle, title)
                .eq(PaperEntity::getOwnerId, userId)
                .count();
        return count != null && count > 0;
    }

    /**
     * 获取试卷并执行全套校验（存在性、权限、状态）
     *
     * @param paperId        试卷ID
     * @param userId         当前操作用户ID
     * @param expectedStatus 期望的状态 (例如 0=草稿。如果传 null 则跳过状态检查)
     * @return 校验通过的实体
     * @throws ApplicationException 校验失败时抛出对应异常
     */
    public PaperEntity getAndValidate(Long paperId, Long userId, PaperStatusEnum expectedStatus) {
        // 1. 基础查询
        PaperEntity paperEntity = paperRepository.getById(paperId);

        // 2. 校验是否存在
        if (paperEntity == null) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_NOT_FOUND);
        }

        // 3. 校验权限 (是否是试卷拥有者)
        if (!userId.equals(paperEntity.getOwnerId())) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }

        // 4. 校验状态 (当 expectedStatus 为 null 时不校验)
        if (expectedStatus != null && !Objects.equals(paperEntity.getStatus(), expectedStatus.getCode())) {
            throw new ApplicationException(PaperErrorCodeEnum.PAPER_STATUS_ERROR,"试卷状态错误，当前状态：" + paperEntity.getStatus());
        }

        return paperEntity;
    }
}
