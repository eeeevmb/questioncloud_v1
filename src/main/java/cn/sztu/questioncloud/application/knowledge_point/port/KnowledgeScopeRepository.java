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

    KnowledgeScopeEntity getByScopeName(String scopeName);

    KnowledgeScopeEntity getById(Long id);

    void save(KnowledgeScopeEntity entity);

    void update(KnowledgeScopeEntity entity);

    void delete(Long id);
}
