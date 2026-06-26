package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;

import java.util.List;

public interface KnowledgePointRepository {
    /**
     * 根据ID获取知识点实体
     *
     * @param id 知识点ID
     * @return 知识点实体
     */
    KnowledgePointEntity getById(Long id);

    /**
     * 根据名字查询知识点实体列表
     *
     * @param subject       学科
     * @param name          查找的名字
     * @return 知识点实体列表
     */
    List<KnowledgePointEntity> listByCanonicalNameOrAlias(
            Long knowledgeScopeId, String name);

    /**
     * 查询需要补全的知识点实体列表
     *
     * @param limit 查询数量限制
     * @return 需要补全的知识点实体列表
     */
    List<KnowledgePointEntity> listNeedEnrich(Integer limit);

    /**
     * 查询所有未删除的知识点实体。
     *
     * @return 知识点实体列表
     */
    List<KnowledgePointEntity> listAllActive();



    /**
     * 保存知识点实体
     *
     * @param entity 知识点实体
     */
    void save(KnowledgePointEntity entity);

    /**
     * 更新知识点实体
     *
     * @param entity 知识点实体
     */
    void update(KnowledgePointEntity entity);

    /**
     * 删除知识点实体
     *
     * @param id 知识点ID
     */
    void delete(Long id);
}
