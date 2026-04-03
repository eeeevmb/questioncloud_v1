package cn.sztu.questioncloud.application.user.port;

import cn.sztu.questioncloud.application.user.enums.VerificationTypeEnum;

public interface UserVerificationPort {

    /**
     * 创建验证码并保存，返回验证码明文
     */
    String createVerificationToken(String identify, VerificationTypeEnum type);

    /**
     * 获取当前验证码
     */
    String getVerificationToken(String identify, VerificationTypeEnum type);

    /**
     * 校验验证码
     */
    boolean verifyCode(String identify, String code, VerificationTypeEnum type);

    /**
     * 删除验证码
     */
    void deleteVerificationToken(String identify, VerificationTypeEnum type);

    /**
     * 是否达到发送限制
     */
    boolean hasRateLimitReached(String identify, VerificationTypeEnum type);
}
