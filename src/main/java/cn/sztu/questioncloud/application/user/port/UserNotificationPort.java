package cn.sztu.questioncloud.application.user.port;

import cn.sztu.questioncloud.application.user.enums.VerificationTypeEnum;

public interface UserNotificationPort {

    /**
     * 发送验证码邮件
     *
     * @param email 邮箱
     * @param code 验证码
     * @param verificationType 验证码类型
     */
    void sendVerificationCode(String email, String code, VerificationTypeEnum verificationType);
}