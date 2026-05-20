package cn.sztu.questioncloud.infrastructure.adapter.question.messaging;

import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.service.QuestionVectorizeService;
import cn.sztu.questioncloud.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuestionEventConsumer {
    private static final org.slf4j.Logger MQ_LOG = org.slf4j.LoggerFactory.getLogger("MQ_LOG");

    private final QuestionVectorizeService questionVectorizeService;

    public QuestionEventConsumer(QuestionVectorizeService questionVectorizeService) {
        this.questionVectorizeService = questionVectorizeService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUESTION_QUEUE_NAME, concurrency = "4")
    public void handleQuestionEvent(QuestionEventMessage message,
                                    @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey,
                                    @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
        long start = System.currentTimeMillis();
        try {
            switch (routingKey) {
                case RabbitMQConfig.QUESTION_CREATED_KEY,
                     RabbitMQConfig.QUESTION_UPDATED_KEY ->
                    questionVectorizeService.onQuestionUpsert(message);
                case RabbitMQConfig.QUESTION_DELETED_KEY ->
                    questionVectorizeService.onQuestionDeleted(message);
            }
            MQ_LOG.info("queue={} exchange={} routingKey={} messageId={} messageType={} businessId={} retryCount={} success={} costMs={} errorMessage={}",
                    RabbitMQConfig.QUESTION_QUEUE_NAME,
                    RabbitMQConfig.QUESTION_EXCHANGE_NAME,
                    routingKey,
                    messageId == null ? "" : messageId,
                    "QuestionEventMessage",
                    message == null ? "" : message.getQuestionId(),
                    "-",
                    true,
                    System.currentTimeMillis() - start,
                    "");
        } catch (RuntimeException e) {
            MQ_LOG.info("queue={} exchange={} routingKey={} messageId={} messageType={} businessId={} retryCount={} success={} costMs={} errorMessage={}",
                    RabbitMQConfig.QUESTION_QUEUE_NAME,
                    RabbitMQConfig.QUESTION_EXCHANGE_NAME,
                    routingKey,
                    messageId == null ? "" : messageId,
                    "QuestionEventMessage",
                    message == null ? "" : message.getQuestionId(),
                    "-",
                    false,
                    System.currentTimeMillis() - start,
                    e.getMessage());
            throw e;
        }
    }
}
