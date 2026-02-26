package cn.sztu.questioncloud.application.question.port;

import java.util.List;

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

    /**
     * 同一用户下是否存在同名题集
     *
     * @param ownerId 用户ID
     * @param name    题集名称
     * @return 是否存在
     */
    boolean existsByOwnerIdAndName(Long ownerId, String name);

    /**
     * 查询同一用户下已存在的题集名称
     *
     * @param ownerId 用户ID
     * @param names   题集名称集合
     * @return 已存在的名称列表
     */
    List<String> findExistingNames(Long ownerId, List<String> names);
}
