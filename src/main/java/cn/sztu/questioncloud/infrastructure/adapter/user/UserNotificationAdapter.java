package cn.sztu.questioncloud.infrastructure.adapter.user;

import cn.sztu.questioncloud.application.user.enums.VerificationTypeEnum;
import cn.sztu.questioncloud.application.user.port.UserNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserNotificationAdapter implements UserNotificationPort {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    @Override
    public void sendVerificationCode(String email, String code, VerificationTypeEnum verificationType) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject(buildSubject(verificationType));
        message.setText(buildContent(code, verificationType));
        mailSender.send(message);
    }

    // ==== 私有方法 ====

    private String buildSubject(VerificationTypeEnum verificationType) {
        return switch (verificationType) {
            case REGISTER -> "【QuestionCloud】注册验证码";
        };
    }

    private String buildContent(String code, VerificationTypeEnum verificationType) {
        return switch (verificationType) {
            case REGISTER -> "您好，您的注册验证码是：" + code + "，5分钟内有效。";
        };
    }

}
