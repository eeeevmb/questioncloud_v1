package cn.sztu.questioncloud.application.knowledge_point.service;

import cn.sztu.questioncloud.application.knowledge_point.dto.DirectoryKnowledgeExtractDTO;
import cn.sztu.questioncloud.application.knowledge_point.dto.QuestionKnowledgeExtractDTO;

import java.util.List;
import java.util.Map;

/*
 * 和知识点提取相关的 AI 服务接口
 */
public interface KnowledgeExtractService {
    /**
     * 从目录文本中提取知识点
     *
     * @param directoryText 目录文本
     * @return 目录知识点列表
     */
    List<DirectoryKnowledgeExtractDTO> extractFromDirectoryText(String directoryText);

    /**
     * 从题目版本中提取知识点，并建立题目-知识点关系
     *
     * @param questionVersionId 题目版本 ID
     * @return 题目知识点列表
     */
    List<QuestionKnowledgeExtractDTO> extractFromQuestionAndBind(Long questionVersionId);

    /**
     * AI 补充缺少详细信息的知识点
     *
     * @param knowledgePointIds 知识点 ID 列表
     */
    void enrichMissingDetails(List<Long> knowledgePointIds);

    // ========================================

    /**
     * 根据题目版本判断其最适合归属的知识点领域
     *
     * @param questionVersionId 题目版本 ID
     * @return 知识点领域名称
     */
    List<String> identifyKnowledgeDomains(Long questionVersionId);

    /**
     * 从题集中提取每一道题目的知识点，并建立题目-知识点关系
     *
     * @param collectionId 题集 ID
     * @return 题集内各题的知识点提取结果
     */
    Map<Long, List<QuestionKnowledgeExtractDTO>> extractAndBindCollection(Long collectionId);

    /**
     * AI 补充缺少详细信息的知识点
     *
     * @param limit 每次补全的知识点数量限制
     */
    void enrichMissingDetails(Integer limit);
}
