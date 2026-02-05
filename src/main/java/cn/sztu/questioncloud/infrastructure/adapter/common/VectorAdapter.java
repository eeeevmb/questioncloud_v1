package cn.sztu.questioncloud.infrastructure.adapter.common;

import cn.sztu.questioncloud.application.common.port.VectorPort;
import cn.sztu.questioncloud.infrastructure.common.ai.model.VectorizationRequest;
import cn.sztu.questioncloud.infrastructure.common.ai.service.VectorizationTool;
import org.springframework.stereotype.Component;

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
    public String add(String text, Map<String, String> metadata) {
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
    public void upsert(String vectorId, String text, Map<String, String> metadata) {
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
}
