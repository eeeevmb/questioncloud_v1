package cn.sztu.questioncloud.application.user.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;

public interface UserAccountRepository {

    // ===== 查询操作 =====

    /**
     * 根据用户名查找用户实体
     * @param username 用户名
     * @return 用户实体的Optional封装
     */
    Optional<UserAccountEntity> findByUsername(String username);

    /**
     * 根据用户id查找用户实体
     * @param id 用户id
     * @return 用户实体的Optional封装
     */
    Optional<UserAccountEntity> findById(Long id);

    /**
     * 根据用户邮箱查找用户实体
     * @param email 用户邮箱
     * @return 用户实体的Optional封装
     */
    Optional<UserAccountEntity> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户实体
     * @param account 用户名或邮箱
     * @return 用户实体的Optional封装
     */
    Optional<UserAccountEntity> findByAccount(String account);

    // ===== 写入操作 =====

    /**
     * 保存新用户
     * @param userAccount 用户实体
     */
    void save(UserAccountEntity userAccount);

    /**
     * 更新头像url
     *
     * @param userId 用户id
     * @param url    头像url
     */
    void updateAvatar(Long userId, String url);
}
