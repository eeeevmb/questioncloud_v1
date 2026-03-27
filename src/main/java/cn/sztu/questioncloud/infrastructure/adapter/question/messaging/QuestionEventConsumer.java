package cn.sztu.questioncloud.infrastructure.adapter.question.messaging;

import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.application.question.service.QuestionVectorizeService;
import cn.sztu.questioncloud.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class QuestionEventConsumer {
    private final QuestionVectorizeService questionVectorizeService;

    public QuestionEventConsumer(QuestionVectorizeService questionVectorizeService) {
        this.questionVectorizeService = questionVectorizeService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUESTION_QUEUE_NAME, concurrency = "4")
    public void handleQuestionEvent(QuestionEventMessage message,
                                    @Header(AmqpHeaders.RECEIVED_ROUTING_KEY) String routingKey) {
        switch (routingKey) {
            case RabbitMQConfig.QUESTION_CREATED_KEY,
                 RabbitMQConfig.QUESTION_UPDATED_KEY ->
                questionVectorizeService.onQuestionUpsert(message);
            case RabbitMQConfig.QUESTION_DELETED_KEY ->
                questionVectorizeService.onQuestionDeleted(message);
        }

    }
}
