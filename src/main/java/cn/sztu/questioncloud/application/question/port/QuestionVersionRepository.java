package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;

import java.util.List;
import java.util.Optional;

public interface QuestionVersionRepository {
    /**
     * 根据题目ID查询最新题目版本实体
     *
     * @param questionId 题目ID
     * @return 最新版本实体
     */
    QuestionVersionEntity getCurrentVersionByQuestionId(Long questionId);

    // ===== 写入操作 =====

    /**
     * 保存题目版本实体
     *
     * @param entity 题目版本实体
     * @return 题目版本ID
     */
    Optional<Long> save(QuestionVersionEntity entity);

    /**
     * 批量保存题目版本实体
     *
     * @param entities 题目版本实体
     */
    void batchSave(List<QuestionVersionEntity> entities);
}
