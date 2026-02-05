package cn.sztu.questioncloud.application.question.messaging;

import cn.sztu.questioncloud.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
public class QuestionEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public QuestionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    // 发布时机：
    // 1.创建题目，同步写入向量库记录 2.更新题目版本，向量库删除旧记录，新增新记录 3.删除题目，同步删除
    public void publishCreated(QuestionEventMessage message) {
        publishAfterCommit(RabbitMQConfig.QUESTION_EXCHANGE_NAME,
                RabbitMQConfig.QUESTION_CREATED_KEY,
                message);
    }

    public void publishUpdated(QuestionEventMessage message) {
        publishAfterCommit(RabbitMQConfig.QUESTION_EXCHANGE_NAME,
                RabbitMQConfig.QUESTION_UPDATED_KEY,
                message);
    }

    public void publishDeleted(QuestionEventMessage message) {
        publishAfterCommit(RabbitMQConfig.QUESTION_EXCHANGE_NAME,
                RabbitMQConfig.QUESTION_DELETED_KEY,
                message);
    }

    private void publishAfterCommit(String exchange, String routingKey, QuestionEventMessage msg) {
        Runnable sendTask = () -> {
            rabbitTemplate.convertAndSend(exchange, routingKey, msg);
            log.info("发布题目领域事件. rk={}, msg={}", routingKey, msg);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
        } else {
            // 非事务场景（例如单元测试/手动调用）
            sendTask.run();
        }
    }

}
