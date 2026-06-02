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
     * 根据学科和名字尝试地查询知识点实体
     *
     * @param subject       学科
     * @param name          查找的名字
     * @return 知识点实体列表
     */
    KnowledgePointEntity searchCanonicalNameOrAlias(
            String subject, String name);

    /**
     * 根据名字尝试地查询知识点实体列表
     *
     * @param subject       学科
     * @param name          查找的名字
     * @return 知识点实体列表
     */
    List<KnowledgePointEntity> listByCanonicalNameOrAlias(
            String subject, String name);

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
}
