package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;

import java.util.List;

public interface KnowledgeQuestionRelRepository {
    /**
     * 根据题目版本ID查询关联的知识点关系
     *
     * @param questionVersionId 题目版本ID
     * @return 关联的知识点关系列表
     */
    List<KnowledgeQuestionRelEntity> listByQuestionVersionId(Long questionVersionId);

    /**
     * 根据题目ID删除关联的知识点关系
     *
     * @param questionVersionId 题目ID
     */
    void deleteRelationsByQuestionVersionId(Long questionVersionId);

    /**
     * 保存单条题目知识点关联关系
     * 适合新增关系
     *
     * @param entity 题目知识点关联实体
     */
    void save(KnowledgeQuestionRelEntity entity);

    /**
     * 批量保存题目知识点关联关系
     * 适合新建题目
     *
     * @param entities 题目知识点关联实体列表
     */
    void batchSave(List<KnowledgeQuestionRelEntity> entities);

    /**
     * 删除题目知识点关联关系
     * 适合更新题目时，先删除旧关系再新增新关系的场景
     *
     * @param knowledgePointId 题目知识点关联关系ID
     */
    void deleteByKnowledgePointId(Long knowledgePointId);

    /**
     * 根据题目ID删除关联关系
     *
     * @param questionId 题目ID
     */
    void deleteByQuestionId(Long questionId);

    /**
     * 判断某知识点是否仍被题目引用
     *
     * @param knowledgePointId 知识点ID
     * @return 是否存在题目关联
     */
    boolean existsByKnowledgePointId(Long knowledgePointId);
}
