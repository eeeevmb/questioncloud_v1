package cn.sztu.questioncloud.infrastructure.adapter.question.messaging;

import cn.sztu.questioncloud.application.question.service.QuestionCollectionService;
import cn.sztu.questioncloud.application.user.messaging.UserRegisteredMessage;
import cn.sztu.questioncloud.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * MQ消费者，监听user.registered消息
 */
@Slf4j
@Component
public class UserEventConsumer {
    private static final org.slf4j.Logger MQ_LOG = org.slf4j.LoggerFactory.getLogger("MQ_LOG");

    private final QuestionCollectionService questionCollectionService;

    public UserEventConsumer(QuestionCollectionService questionCollectionService) {
        this.questionCollectionService = questionCollectionService;
    }

    @RabbitListener(queues = RabbitMQConfig.USER_QUEUE_NAME)
    public void handleUserRegistered(UserRegisteredMessage message,
                                     @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        long start = System.currentTimeMillis();
        try {
            log.info("消费者收到用户注册消息，用户ID={}，注册时间={}", message.userId(), message.occurredAt());
            questionCollectionService.createDefaultCollection(message.userId());
            log.info("默认题集已创建，用户ID={}", message.userId());
            MQ_LOG.info("queue={} exchange={} routingKey={} messageId={} messageType={} businessId={} retryCount={} success={} costMs={} errorMessage={}",
                    RabbitMQConfig.USER_QUEUE_NAME,
                    RabbitMQConfig.USER_EXCHANGE_NAME,
                    RabbitMQConfig.USER_REGISTERED_KEY,
                    messageId == null ? "" : messageId,
                    "UserRegisteredMessage",
                    message == null ? "" : message.userId(),
                    "-",
                    true,
                    System.currentTimeMillis() - start,
                    "");
        } catch (RuntimeException e) {
            MQ_LOG.info("queue={} exchange={} routingKey={} messageId={} messageType={} businessId={} retryCount={} success={} costMs={} errorMessage={}",
                    RabbitMQConfig.USER_QUEUE_NAME,
                    RabbitMQConfig.USER_EXCHANGE_NAME,
                    RabbitMQConfig.USER_REGISTERED_KEY,
                    messageId == null ? "" : messageId,
                    "UserRegisteredMessage",
                    message == null ? "" : message.userId(),
                    "-",
                    false,
                    System.currentTimeMillis() - start,
                    e.getMessage());
            throw e;
        }
    }
}
