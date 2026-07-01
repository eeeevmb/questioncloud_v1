package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgeQuestionSearchDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeTagDTO;
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

    List<QuestionKnowledgeTagDTO> listQuestionKnowledgeTags(Long questionVersionId);

    /**
     * 根据知识点列表在指定题集中查询关联题目
     *
     * @param knowledgePointIds 知识点ID列表
     * @param collectionIds 题集ID列表
     * @param topK 返回数量限制
     * @param typeCode 题型筛选
     * @param difficultyMin 难度下限
     * @param difficultyMax 难度上限
     * @return 题目候选列表
     */
    List<KnowledgeQuestionSearchDTO> searchQuestionsByKnowledgePoints(List<Long> knowledgePointIds,
                                                                      List<Long> collectionIds,
                                                                      Integer topK,
                                                                      String typeCode,
                                                                      Double difficultyMin,
                                                                      Double difficultyMax);
}
