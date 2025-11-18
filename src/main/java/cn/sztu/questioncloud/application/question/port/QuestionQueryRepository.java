package cn.sztu.questioncloud.application.question.port;

/**
 * 题目模块查询仓储接口
 */
public interface QuestionQueryRepository {

    /**
     * 查询题集内题目总数
     *
     * @param collectionId 题集ID
     * @return 题集内题目总数
     */
    Integer CountCollectionItemsById(Long collectionId);

    /**
     * 查询题集内最大顺序值
     *
     * @param collectionId 题集ID
     * @return 顺序值
     */
    Integer getMaxOrdinal(Long collectionId);
}
