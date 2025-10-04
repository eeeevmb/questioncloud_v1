package cn.sztu.questioncloud.common.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码加密工具类
 * 提供密码加密和验证功能
 *
 * @author LinSanQi
 */
public class PasswordEncryptionUtil {

    /**
     * 加密密码
     *
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encrypt(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return BCrypt.hashpw(rawPassword);
    }

    /**
     * 验证密码
     *
     * @param rawPassword    原始密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (Exception e) {
            // 处理格式无效等异常情况
            return false;
        }
    }

    /**
     * 检查密码是否已经被加密
     *
     * @param password 密码
     * @return 是否已加密
     */
    public static boolean isEncrypted(String password) {
        return password != null && password.startsWith("$2a$");
    }
}