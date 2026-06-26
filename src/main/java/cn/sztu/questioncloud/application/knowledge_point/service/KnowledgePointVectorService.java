package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorDetailVO;
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

    /**
     * 重新上传所有知识点
     *
     * @return 索引的知识点数量
     */
    int reindexAll();

    /**
     * 查询相关知识点
     *
     * @param subject  学科
     * @param query    用户的自然语言描述
     * @param topK     返回数量上限，可为null
     * @param minScore 最小相似度阈值，可为null
     * @return 命中的知识点列表
     */
    List<KnowledgePointVectorSearchVO> searchCandidates(String subject, String query, Integer topK, Double minScore);

    /**
     * 根据知识点ID查询向量库中实际保存的文档和元数据，仅调试使用
     *
     * @param knowledgePointId 知识点ID
     * @return 向量明细
     */
    KnowledgePointVectorDetailVO getVectorDetail(Long knowledgePointId);
}
