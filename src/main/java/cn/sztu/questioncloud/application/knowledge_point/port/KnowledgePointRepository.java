package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;

import java.util.List;

public interface KnowledgePointRepository {
    /**
     * 根据标准名或别名查询知识点实体列表
     *
     * @param name 查找的名称
     * @return 知识点实体列表
     */
    List<KnowledgePointEntity> listByCanonicalNameOrAlias(String name);

    /**
     * 查询需要补全的知识点实体列表
     *
     * @param limit 查询数量限制
     * @return 需要补全的知识点实体列表
     */
    List<KnowledgePointEntity> listNeedEnrich(Integer limit);

    /**
     * 查询所有未删除的知识点实体
     *
     * @return 知识点实体列表
     */
    List<KnowledgePointEntity> listAllActive();

    // === BASIC ===

    KnowledgePointEntity getById(Long id);

    void save(KnowledgePointEntity entity);

    void update(KnowledgePointEntity entity);

    void delete(Long id);
}
