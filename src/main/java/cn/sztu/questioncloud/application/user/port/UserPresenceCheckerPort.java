package cn.sztu.questioncloud.application.user.port;

/**
 * 用户存在性检测服务接口
 * == 可以基于布隆过滤器重写，暂时直接sql查询替代 ==
 */

public interface UserPresenceCheckerPort {
    /**
     * 检查用户名是否已存在
     *
     * @param username 用户名
     * @return 存在返回true
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     *
     * @param email 邮箱
     * @return 存在返回true
     */
    boolean existsByEmail(String email);
}
