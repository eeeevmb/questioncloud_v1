package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.application.paper.enums.PaperStatusEnum;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;

public interface PaperCheckRepository {
    /**
     * 校验用户是否存在同名试卷
     *
     * @param title 试卷title
     * @param userId 用户ID
     * @return 是否存在
     */
    boolean existsByTitle(String title, Long userId);

    /**
     * 获取试卷并执行全套校验（存在性、权限、状态）
     *
     * @param paperId        试卷ID
     * @param userId         当前操作用户ID
     * @param expectedStatus 期望的状态 (例如 0=草稿。如果传 null 则跳过状态检查)
     * @return 校验通过的实体
     * @throws ApplicationException 校验失败时抛出对应异常
     */
    PaperEntity getAndValidate(Long paperId, Long userId, PaperStatusEnum expectedStatus);
}
