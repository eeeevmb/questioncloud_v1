package cn.sztu.questioncloud.application.common.port;

import java.util.Map;

public interface VectorPort {
    /**
     * 将文本向量化并存储
     *
     * @param text     文本
     * @param metadata 元数据
     * @return 向量ID
     */
    String add(String text, Map<String, String> metadata);

    /**
     * 将文本向量化并存储
     * 若向量存在即更新，不存在即写入
     *
     * @param vectorId 向量ID
     * @param text     文本
     * @param metadata 元数据
     */
    void upsert(String vectorId, String text, Map<String, String> metadata);

    /**
     * 根据向量ID删除单个向量
     *
     * @param vectorId 向量ID
     */
    void delete(String vectorId);
}
