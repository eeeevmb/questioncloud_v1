package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.testDTO.KnowledgePointVectorOverviewVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorDetailVO;
import cn.sztu.questioncloud.web.rest.v1.knowledge_point.vo.KnowledgePointVectorSearchVO;

import java.util.List;

public interface KnowledgePointVectorService {

    /**
     * 知识点向量化入库
     */
    void upsert(KnowledgePointEntity entity);

    /**
     * 从向量库中删除知识点
     */
    void delete(Long knowledgePointId);

    /**
     * 重新上传所有知识点
     */
    int reindexAll();

    /**
     * 搜索知识点向量候选
     */
    List<KnowledgePointVectorSearchVO> searchCandidates(List<String> knowledgeScopes,
                                                        String query,
                                                        Integer topK,
                                                        Double minScore);

    /**
     * 根据知识点ID查询向量库中实际保存的文档和元数据，仅调试使用
     */
    KnowledgePointVectorDetailVO getVectorDetail(Long knowledgePointId);

    /**
     * 清空知识点相关向量，仅删除知识点向量，不影响题目向量
     */
    int clearKnowledgePointVectors();

    /**
     * 查询知识点向量库概览
     */
    KnowledgePointVectorOverviewVO getKnowledgePointVectorOverview();
}
