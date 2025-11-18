package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;

import java.util.List;

public interface CollectionItemRepository {

    // ===== 写入操作 =====

    /**
     * 将题目存入题集
     *
     * @param collectionItem 题集内容
     */
    void save(CollectionItem collectionItem);

    /**
     * 批量存入题集
     *
     * @param items 题集内容
     */
    void batchSave(List<CollectionItem> items);
}
