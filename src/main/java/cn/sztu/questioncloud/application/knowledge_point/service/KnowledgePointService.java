package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;

import java.util.List;

/*
 * 用于处理知识点的 CRUD 任务
 */
public interface KnowledgePointService {
    /**
     * 增加知识点领域
     *
     * @param scopeName 知识点领域名称
     */
    void addKnowledgeScope(String scopeName);

    /**
     * 删除知识点及其相关关系
     *
     * @param knowledgePointIds 要删除的知识点 ID 列表
     */
    void deleteKnowledgePoints(List<Long> knowledgePointIds);



    /**
     * 进行知识点实体和关系的存储。
     *
     * @param questionId        题目 ID
     * @param questionVersionId 题目版本 ID
     * @param extractedPoints   AI 提取出的知识点列表
     */
    void bindExtractedKnowledgePoints(
            Long questionId,
            Long questionVersionId,
            List<QuestionKnowledgeExtractDTO> extractedPoints
    );


    /**
     * 尝试在数据库中找到匹配的知识点实体，否则进行存储
     *
     * @param dto AI 提取出的候选知识点信息
     * @return 找到或创建的知识点实体
     */
    KnowledgePointEntity findOrCreateKnowledgePoint(QuestionKnowledgeExtractDTO dto);
}
