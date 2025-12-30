package cn.sztu.questioncloud.application.paper.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.web.rest.v1.paper.vo.PaperItemVO;

import java.util.List;

public interface PaperItemRepository {
    /**
     * 根据试卷ID删除试卷下所有试题关联
     *
     * @param paperId 试卷ID
     */
    void deleteItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID获取试卷下所有试题关联实体
     *
     * @param paperId 试卷ID
     * @return 试卷题目实体列表(由题序排列)
     */
    List<PaperItemEntity> getItemsByPaperId(Long paperId);

    /**
     * 根据试卷ID获取试卷下所有实体详情
     *
     * @param paperId 试卷ID
     * @return 试卷题目详情实体列表
     */
    List<PaperItemVO> getDetailedItemsByPaperId(Long paperId);

    // ===== 写入操作 =====
    /**
     * 根据试卷ID保存单个试题联系
     *
     * @param entity 试卷题目实体
     */
    void saveItem(PaperItemEntity entity);

    /**
     * 根据试卷ID保存多个试题联系(常用)
     *
     * @param entities 试卷题目实体列表
     */
    void saveBatchItems(List<PaperItemEntity> entities);

}
