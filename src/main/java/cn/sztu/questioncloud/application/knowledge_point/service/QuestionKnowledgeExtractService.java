package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;

import java.util.List;

public interface QuestionKnowledgeExtractService {
    /**
     * 从题目版本中提取知识点，并建立题目-知识点关系。
     *
     * @param questionVersionId 题目版本ID
     */
    List<KnowledgePointExtractDTO> extractAndBind(Long questionVersionId);
}
