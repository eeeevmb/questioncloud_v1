package cn.sztu.questioncloud.application.question.port;

public interface CollectionPresenceCheckerPort {
    /**
     * 检查默认题集是否已存在
     *
     * @param userId 用户ID
     * @return 存在返回true
     */
    boolean existsDefaultByUserId(Long userId);

    /**
     * 检查题集是否已存在
     *
     * @param collectionId 题集ID
     * @return 存在返回true
     */
    boolean existsById(Long collectionId);
}