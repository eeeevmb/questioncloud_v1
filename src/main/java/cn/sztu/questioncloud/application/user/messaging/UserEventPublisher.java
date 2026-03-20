package cn.sztu.questioncloud.application.user.messaging;

import cn.sztu.questioncloud.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

/**
 * 用户领域事件的消息发布器
 *
 * @author Codex
 */
@Slf4j
@Component
public class UserEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public UserEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发布注册消息；事务存在时在提交后发送
     *
     * @param userId 新注册用户ID
     */
    public void publishUserRegistered(Long userId) {
        Runnable sendTask = () -> doPublish(userId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
        } else {
            sendTask.run();
        }
    }

    private void doPublish(Long userId) {
        UserRegisteredMessage message = new UserRegisteredMessage(userId, LocalDateTime.now());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.USER_EXCHANGE_NAME,
                RabbitMQConfig.USER_REGISTERED_KEY,
                message
        );
        log.info("发送用户注册消息成功，userId={}", userId);
    }
}
