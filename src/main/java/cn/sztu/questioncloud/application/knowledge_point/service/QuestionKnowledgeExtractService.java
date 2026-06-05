package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.KnowledgePointExtractDTO;

import java.util.List;
import java.util.Map;

/*
 * 用于从题目版本中提取知识点的服务接口。
 */
public interface QuestionKnowledgeExtractService {
    /**
     * 从题目版本中提取知识点，并建立题目-知识点关系。
     *
     * @param questionVersionId 题目版本ID
     */
    List<KnowledgePointExtractDTO> extractAndBind(Long questionVersionId);

    /**
     * 从题目中提取知识点，并建立题目-知识点关系。
     *
     * @param questionId 题目版本ID
     */
    List<KnowledgePointExtractDTO> extractAndBindCurrentQuestion(Long questionId);

    /**
     * 从题集中提取每一道题目的知识点，并建立题目-知识点关系。
     *
     * @param collectionId 题集ID列表
     */
    Map<Long, List<KnowledgePointExtractDTO>> extractAndBindCollection(Long collectionId);

    /**
     * AI补充缺乏详细信息的知识点。
     *
     * @param limit 每次补全的知识点数量限制，避免一次性处理过多数据
     */
    void enrichMissingDetails(Integer limit);

    /**
     * AI补充缺乏详细信息的知识点。
     *
     * @param knowledgePointId 知识点ID
     */
    void enrichMissingDetails(List<Long> knowledgePointId);
}
