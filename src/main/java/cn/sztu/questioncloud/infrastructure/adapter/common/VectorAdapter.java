package cn.sztu.questioncloud.infrastructure.adapter.common;

import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.infrastructure.common.ai.model.VectorizationRequest;
import cn.sztu.questioncloud.infrastructure.common.ai.service.VectorizationTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class VectorAdapter implements VectorPort {
    private final VectorizationTool vectorizationTool;

    public VectorAdapter(VectorizationTool vectorizationTool) {
        this.vectorizationTool = vectorizationTool;
    }

    /**
     * 将文本向量化并存储
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 向量ID
     */
    @Override
    public String add(String text, Map<String, Object> metadata) {
        return vectorizationTool.add(new VectorizationRequest(text, metadata));
    }

    /**
     * 将文本向量化并存储
     * 若向量存在即更新，不存在即写入
     *
     * @param vectorId 向量ID
     * @param text     文本
     * @param metadata 元数据
     */
    @Override
    public void upsert(String vectorId, String text, Map<String, Object> metadata) {
        vectorizationTool.upsert(vectorId, new VectorizationRequest(text, metadata));
    }

    /**
     * 根据向量ID删除单个向量
     *
     * @param vectorId 向量ID
     */
    @Override
    public void delete(String vectorId) {
        vectorizationTool.delete(vectorId);
    }

    /**
     * 基于向量相似度在 EmbeddingStore 中检索，支持按元数据进行过滤
     *
     * @param query 向量检索参数，包含查询文本、TopK、相似度阈值以及可选的元数据过滤条件
     * @return 向量检索结果
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(SearchQuery query) {
        return vectorizationTool.search(query);
    }

}
