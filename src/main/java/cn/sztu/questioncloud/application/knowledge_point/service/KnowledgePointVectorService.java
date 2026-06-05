package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;

import java.util.List;

public interface KnowledgePointVectorService {
    /**
     * 知识点向量化入库
     *
     * @param entity 知识点实体
     */
    void upsert(KnowledgePointEntity entity);

    /**
     * 从向量库中删除知识点
     *
     * @param knowledgePointId 知识点ID
     */
    void delete(Long knowledgePointId);

    int reindexAll();

    /**
     * 查询相关知识点ID列表
     *
     * @param subject  学科
     * @param query    用户的自然语言描述
     * @param topK     可 null,返回的候选知识点数量上限，默认为 5
     * @param minScore 可 null,候选知识点的最小相似度分数，默认为 0.5
     * @return 命中的知识点ID列表，按照相似度从高到低排序
     */
    List<KnowledgePointVectorSearchVO> searchCandidates(String subject, String query, Integer topK, Double minScore);
}
