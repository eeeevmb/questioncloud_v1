package cn.sztu.questioncloud.common.util;

import java.security.SecureRandom;

// 用于生成班级代码的工具类
// 生成一个包含5个字符的随机字符串，字符集为大写字母和数字
// 例如：A1B2C 或者 3D4E5
// 实现比较简单，后续可以调整算法。
public final class ClassCodeGenerator {
    private ClassCodeGenerator() {}

    private static final SecureRandom random = new SecureRandom();
    private static final String CHAR_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static String generateCode() {
        StringBuilder code = new StringBuilder();
        int length = CHAR_POOL.length();
        for (int i = 0; i < 5; i++) {
            code.append(CHAR_POOL.charAt(random.nextInt(length)));
        }
        return code.toString();
    }
}
