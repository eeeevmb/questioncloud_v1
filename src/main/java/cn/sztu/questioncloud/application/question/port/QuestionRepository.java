package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;

import java.util.List;

public interface QuestionRepository {
    // ===== 写入操作 =====
    /**
     * 保存题目实体
     *
     * @param entity 题目实体
     */
    void save(QuestionEntity entity);

    /**
     * 批量保存题目实体
     *
     * @param entities 实体列表
     */
    void batchSave(List<QuestionEntity> entities);
}
