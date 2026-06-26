package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionScopeRelEntity;

public interface QuestionScopeRelRepository {
    /**
     * 根据ID获取题目-知识点领域关系实体
     *
     * @param id 关系ID
     * @return 关系实体
     */
    QuestionScopeRelEntity getById(Long id);

    /**
     * 保存题目-知识点领域关系实体
     *
     * @param entity 关系实体
     */
    void save(QuestionScopeRelEntity entity);

    /**
     * 更新题目-知识点领域关系实体
     *
     * @param entity 关系实体
     */
    void update(QuestionScopeRelEntity entity);

    /**
     * 删除题目-知识点领域关系实体
     *
     * @param id 关系ID
     */
    void delete(Long id);
}
