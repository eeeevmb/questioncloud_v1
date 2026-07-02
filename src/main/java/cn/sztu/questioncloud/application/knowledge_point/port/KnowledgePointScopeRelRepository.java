package cn.sztu.questioncloud.application.knowledge_point.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointScopeRelEntity;

import java.util.List;

public interface KnowledgePointScopeRelRepository {
    /**
     * 判断知识点与知识范围的关系是否存在
     *
     * @param knowledgePointId 知识点ID
     * @param knowledgeScopeId 知识范围ID
     * @return true: 存在, false: 不存在
     */
    boolean exists(Long knowledgePointId, Long knowledgeScopeId);

    /**
     * 根据知识点ID查询知识点与知识范围的关系
     *
     * @param knowledgePointId 知识点ID
     * @return 知识点与知识范围的关系列表
     */
    List<KnowledgePointScopeRelEntity> listByKnowledgePointId(Long knowledgePointId);

    /**
     * 根据知识范围ID查询知识点名称列表
     *
     * @param knowledgeScopeIds 知识范围ID列表
     * @return 知识点名称列表
     */
    List<String> listKnowledgePointNamesByKnowledgeScopeIds(List<Long> knowledgeScopeIds);

    // === BASIC ===
    KnowledgePointScopeRelEntity getById(Long id);

    void save(KnowledgePointScopeRelEntity entity);

    void delete(Long id);

    void deleteByKnowledgePointId(Long knowledgePointId);

    void deleteByKnowledgeScopeId(Long knowledgeScopeId);
}
