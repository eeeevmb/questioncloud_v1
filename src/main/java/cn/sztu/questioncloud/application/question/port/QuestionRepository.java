package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;

import java.util.List;

public interface QuestionRepository {
    /**
     * 根据ID查找题目实体
     *
     * @param questionId 题目ID
     * @return 题目实体
     */
    QuestionEntity getById(Long questionId);

    /**
     * 根据实体更新题目
     *
     * @param questionEntity 题目实体
     */
    void update(QuestionEntity questionEntity);

    /**
     * 根据ID删除题目
     *
     * @param questionId 题目ID
     */
    void delete(Long questionId);

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
