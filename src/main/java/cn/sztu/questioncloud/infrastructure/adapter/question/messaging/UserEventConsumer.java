package cn.sztu.questioncloud.infrastructure.adapter.question.messaging;

import cn.sztu.questioncloud.application.question.service.QuestionAppService;
import cn.sztu.questioncloud.application.user.messaging.UserRegisteredMessage;
import cn.sztu.questioncloud.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * MQ消费者，监听user.registered消息
 */
@Slf4j
@Component
public class UserEventConsumer {
    private final QuestionAppService questionAppService;

    public UserEventConsumer(QuestionAppService questionAppService) {
        this.questionAppService = questionAppService;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_QUEUE_NAME)
    public void handleUserRegistered(UserRegisteredMessage message) {
        log.info("消费者收到用户注册消息，用户ID={}，注册时间={}", message.userId(), message.occurredAt());
        questionAppService.createDefaultCollection(message.userId());
        log.info("默认题集已创建，用户ID={}", message.userId());
    }
}
