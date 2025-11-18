package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;

import java.util.Optional;

public interface QuestionCollectionRepository {

    // ===== 查询操作 =====
    /**
     * 根据id查询题集
     *
     * @param collectionId 题集ID
     */
    Optional<QuestionCollectionEntity> findById(Long collectionId);

    // ===== 写入操作 =====
    /**
     * 保存题集实体
     *
     * @param questionCollection 保存的题集对象
     */
    void save(QuestionCollectionEntity questionCollection);

    /**
     * 根据实体更新题集
     *
     * @param questionCollection 题集实体
     */
    void updateByModel(QuestionCollectionEntity questionCollection);

    // ===== 删除操作 =====
    /**
     * 根据ID删除题集，以及题集内容
     *
     * @param collectionId 题集ID
     * @return 是否成功
     */
    boolean deleteById(Long collectionId);
}
