package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;

public interface PaperRepository {
    /**
     * 根据ID查找试卷实体
     *
     * @param paperId 试卷ID
     * @return 试卷实体
     */
    PaperEntity getById(Long paperId);

    /**
     * 目前仅更新试卷的统计类信息 (总分、题数)
     * 此方法不会修改 title、description等元数据
     *
     * @param paper 包含最新统计数据的实体对象
     */
    void updateStatistics(PaperEntity paper);

    /**
     * 根据ID删除试卷
     *
     * @param paperId 试卷ID
     */
    void delete(Long paperId);

    // ===== 写入操作 =====
    /**
     * 保存试卷实体
     *
     * @param entity 试卷实体
     */
    void save(PaperEntity entity);
}
