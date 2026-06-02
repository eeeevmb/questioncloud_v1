package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgePointEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point.KnowledgeQuestionRelEntity;

import java.util.List;

public interface KnowledgePointService {
    /**
     * 处理某个题目版本的知识点提取结果。
     *
     * @param questionId 题目 ID
     * @param questionVersionId 题目版本 ID
     * @param extractedPoints AI 提取出的知识点列表
     */
    List<KnowledgeQuestionRelEntity> bindExtractedKnowledgePoints(
            Long questionId,
            Long questionVersionId,
            List<KnowledgePointExtractDTO> extractedPoints
    );


    /**
     * 根据 AI 提取出的候选知识点，查找或创建知识点实体。
     *
     * @param dto AI 提取出的候选知识点信息
     * @return 找到或创建的知识点实体
     */
    KnowledgePointEntity findOrCreateKnowledgePoint(KnowledgePointExtractDTO dto);

}
