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
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
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

    @Override
    public String add(VectorizationRequest request) {
        TextSegment segment = TextSegment.from(request.getText(), new Metadata(request.getMetadata()));
        Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
        return embeddingStore.add(embeddingResponse.content(), segment);
    }

    @Override
    public void upsert(String vectorId, VectorizationRequest request) {
        TextSegment segment = TextSegment.from(request.getText(), new Metadata(request.getMetadata()));
        Response<Embedding> embeddingResponse = embeddingModel.embed(segment);
        embeddingStore.remove(vectorId);
        embeddingStore.addAll(
                List.of(vectorId),
                List.of(embeddingResponse.content()),
                List.of(segment)
        );
    }

    @Override
    public void delete(String vectorId) {
        embeddingStore.remove(vectorId);
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(SearchQuery query) {
        Response<Embedding> embeddingResponse = embeddingModel.embed(query.getQuery());
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
        if (filter == null) {
            return null;
        }

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

        if (filter.getCollectionIds() != null && !filter.getCollectionIds().isEmpty()) {
            filterList.add(new IsIn("collectionId", filter.getCollectionIds()));
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
        }
        if (filterList.size() == 1) {
            return filterList.getFirst();
        }

        Filter result = filterList.getFirst();
        for (int i = 1; i < filterList.size(); i++) {
            result = new And(result, filterList.get(i));
        }
        return result;
    }
}
