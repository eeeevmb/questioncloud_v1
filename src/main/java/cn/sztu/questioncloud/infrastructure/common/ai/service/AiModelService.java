package cn.sztu.questioncloud.infrastructure.common.ai.service;

import dev.langchain4j.model.embedding.EmbeddingModel;

/**
 * AI 模型获取接口
 *
 * @author eeeevmb
 */
public interface AiModelService {

    /**
     * 根据获取嵌入模型实例
     *
     * @return 模型实例
     */
    EmbeddingModel getEmbeddingModel();
}
