package cn.sztu.questioncloud.infrastructure.common.ai.service;

import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.infrastructure.common.ai.model.VectorizationRequest;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

/**
 * 向量化工具接口
 * 封装文本向量化、存储、更新和检索接口
 *
 * @author eeeevmb
 */
public interface VectorizationTool {

    /**
     * 对单个文本向量化并存储
     *
     * @param request  向量化请求，包含text和metadata
     * @return 存储在向量数据库的ID
     */
    String add(VectorizationRequest request);

    /**
     * 更新已存在的向量，不存在则执行添加操作
     *
     * @param vectorId  向量ID
     * @param request   向量化请求，包含text和metadata
     */
    void upsert(String vectorId, VectorizationRequest request);

    /**
     * 根据向量ID删除单个向量
     *
     * @param vectorId 向量ID
     */
    void delete(String vectorId);

    /**
     * 向量搜索请求，包含元数据过滤等高级功能
     *
     * @param query 搜索请求
     * @return 搜索结果
     */
    EmbeddingSearchResult<TextSegment> search(SearchQuery query);
}
