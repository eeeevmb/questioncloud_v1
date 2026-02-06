package cn.sztu.questioncloud.application.common.port;

import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;

import java.util.Map;

public interface VectorPort {
    /**
     * 将文本向量化并存储
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 向量ID
     */
    String add(String text, Map<String, Object> metadata);

    /**
     * 将文本向量化并存储
     * 若向量存在即更新，不存在即写入
     *
     * @param vectorId 向量ID
     * @param text     文本
     * @param metadata 元数据
     */
    void upsert(String vectorId, String text, Map<String, Object> metadata);

    /**
     * 根据向量ID删除单个向量
     *
     * @param vectorId 向量ID
     */
    void delete(String vectorId);

    /**
     * 基于向量相似度在 EmbeddingStore 中检索，支持按元数据进行过滤
     *
     * @param query 向量检索参数，包含查询文本、TopK、相似度阈值以及可选的元数据过滤条件
     * @return      向量检索结果
     */
    EmbeddingSearchResult<TextSegment> search(SearchQuery query);
}
