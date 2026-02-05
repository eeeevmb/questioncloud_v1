package cn.sztu.questioncloud.infrastructure.common.ai.service.impl;

import cn.sztu.questioncloud.infrastructure.common.ai.service.AiModelService;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

@Service
public class AiModelServiceImpl implements AiModelService {
    private final EmbeddingModel embeddingModel;

    public AiModelServiceImpl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 获取嵌入模型实例
     *
     * @return 模型实例
     */
    @Override
    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel;
    }
}
