package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeScopeEntity;

import java.util.List;

public interface KnowledgeScopeRepository {
    /**
     * 获取所有知识点范围名称
     *
     * @return 知识点范围名称列表
     */
    List<String> getAllScopeNames();

    /**
     * 根据知识点范围名称获取知识点范围实体
     *
     * @param scopeName 知识点范围名称
     * @return 知识点范围实体
     */
    KnowledgeScopeEntity getByScopeName(String scopeName);

    // === BASIC ===

    KnowledgeScopeEntity getById(Long id);

    void save(KnowledgeScopeEntity entity);

    void update(KnowledgeScopeEntity entity);

    void delete(Long id);
}
