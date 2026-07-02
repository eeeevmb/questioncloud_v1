package cn.sztu.questioncloud.application.question.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.CollectionItem;

import java.util.List;
import java.util.Map;

public interface CollectionItemRepository {

    /**
     * 根据题目ID和版本ID查询题集内容实体
     *
     * @param questionId 题目ID
     * @param versionId  版本ID
     * @return 题集内容实体
     */
    CollectionItem findByQuestionIdAndVersionId(Long questionId, Long versionId);

    /**
     * 根据题集ID查询题目版本ID列表
     *
     * @param collectionId 题集ID
     * @return 题目版本ID列表
     */
    List<Long> listVersionIdsByCollectionId(Long collectionId);

    /**
     * @param limit 查询限制数量
     */
    List<Long> listVersionIdsByCollectionId(Long collectionId, Integer limit);

    // ===== 写入操作 =====

    /**
     * 将题目存入题集
     *
     * @param collectionItem 题集内容
     */
    void save(CollectionItem collectionItem);

    /**
     * 根据题目ID删除题集关联
     *
     * @param questionId 题目ID
     */
    void deleteByQuestionId(Long questionId);

    /**
     * 批量存入题集
     *
     * @param items 题集内容
     */
    void batchSave(List<CollectionItem> items);

    /**
     * 根据题目ID更新题集内容的版本ID
     * 用于更新题目后题集内容表同步
     *
     * @param questionId 题目ID
     * @param newVersionId 新题目版本ID
     */
    void syncVersionToAllCollections(Long questionId, Long newVersionId);
}
