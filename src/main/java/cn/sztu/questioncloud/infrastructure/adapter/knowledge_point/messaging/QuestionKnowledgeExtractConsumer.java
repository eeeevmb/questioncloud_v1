package cn.sztu.questioncloud.infrastructure.adapter.knowledge_point.messaging;

import cn.sztu.questioncloud.application.knowledge_point.service.KnowledgeExtractService;
import cn.sztu.questioncloud.application.question.messaging.QuestionEventMessage;
import cn.sztu.questioncloud.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class QuestionKnowledgeExtractConsumer {

    private final KnowledgeExtractService questionKnowledgeExtractService;

    public QuestionKnowledgeExtractConsumer(KnowledgeExtractService knowledgeExtractService) {
        this.questionKnowledgeExtractService = knowledgeExtractService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUESTION_KNOWLEDGE_EXTRACT_QUEUE_NAME)
    public void handleQuestionUpsert(QuestionEventMessage message) {
        if (message == null || message.getVersionId() == null) {
            return;
        }
        questionKnowledgeExtractService.extractFromQuestionAndBind(message.getVersionId());
    }
}
