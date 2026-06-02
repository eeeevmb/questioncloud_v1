package cn.sztu.questioncloud.infrastructure.common.ai.service.impl;

import cn.sztu.questioncloud.application.common.dto.SearchFilter;
import cn.sztu.questioncloud.application.common.dto.SearchQuery;
import cn.sztu.questioncloud.infrastructure.common.ai.model.VectorizationRequest;
import cn.sztu.questioncloud.infrastructure.common.ai.service.VectorizationTool;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo;
import dev.langchain4j.store.embedding.filter.logical.And;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VectorizationToolImpl implements VectorizationTool {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    public VectorizationToolImpl(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        this.embeddingStore = embeddingStore;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 对单个文本向量化并存储
     *
     * @param request        向量化请求，包含text和metadata
     * @return 存储在向量数据库的ID
     */
    @Override
    public String add(VectorizationRequest request) {
        TextSegment segment = TextSegment.from(request.getText(), new Metadata(request.getMetadata()));
        Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
        return embeddingStore.add(embeddingResponse.content(), segment);
    }

    /**
     * 更新已存在的向量，不存在则执行添加操作
     *
     * @param vectorId 向量ID
     * @param request  向量化请求，包含text和metadata
     */
    @Override
    public void upsert(String vectorId, VectorizationRequest request) {
        TextSegment segment = TextSegment.from(request.getText(), new Metadata(request.getMetadata()));
        Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
        embeddingStore.addAll(
                List.of(vectorId),
                List.of(embeddingResponse.content()),
                List.of(segment));
    }

    /**
     * 根据向量ID删除单个向量
     *
     * @param vectorId 向量ID
     */
    @Override
    public void delete(String vectorId) {
        embeddingStore.remove(vectorId);
    }

    /**
     * 向量搜索请求，包含元数据过滤等高级功能
     *
     * @param query 搜索请求
     * @return 搜索结果
     */
    @Override
    public EmbeddingSearchResult<TextSegment> search(SearchQuery query) {
        // 对查询语句向量化处理
        Response<Embedding> embeddingResponse = embeddingModel.embed(query.getQuery());

        // 转换筛选条件
        Filter filter = toFilter(query.getFilter());

        return embeddingStore.search(EmbeddingSearchRequest.builder()
                        .query(query.getQuery())
                        .queryEmbedding(embeddingResponse.content())
                        .maxResults(query.getMaxResult())
                        .minScore(query.getMinScore())
                        .filter(filter)
                        .build());
    }

    private Filter toFilter(SearchFilter filter) {
        if (filter == null) { return null; }

        List<Filter> filterList = new ArrayList<>();

        if (filter.getOwnerId() != null) {
            filterList.add(new IsEqualTo("ownerId", filter.getOwnerId()));
        }

        if (filter.getDocType() != null) {
            filterList.add(new IsEqualTo("docType", filter.getDocType()));
        }

        if (filter.getSubject() != null) {
            filterList.add(new IsEqualTo("subject", filter.getSubject()));
        }

        if (filter.getCollectionId() != null) {
            filterList.add(new IsEqualTo("collectionId", filter.getCollectionId()));
        }

        if (filter.getDifficultyMin() != null) {
            filterList.add(new IsGreaterThanOrEqualTo("difficulty", filter.getDifficultyMin()));
        }

        if (filter.getDifficultyMax() != null) {
            filterList.add(new IsLessThanOrEqualTo("difficulty", filter.getDifficultyMax()));
        }

        if (filter.getTypeCode() != null) {
            filterList.add(new IsEqualTo("typeCode", filter.getTypeCode()));
        }

        if (filterList.isEmpty()) {
            return null;
        } else if (filterList.size() == 1) {
            return filterList.getFirst();
        } else {
            Filter result = filterList.getFirst();
            for (int i = 1; i < filterList.size(); i++) {
                result = new And(result, filterList.get(i));
            }
            return result;
        }
    }
}
