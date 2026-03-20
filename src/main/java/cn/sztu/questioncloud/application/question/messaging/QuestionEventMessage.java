package cn.sztu.questioncloud.application.question.messaging;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 题目创建消息载荷
 */
@Data
@Builder
public class QuestionEventMessage {
    private Long questionId;

    private Long versionId;

    private Long collectionId;

    private Long ownerId;

    private LocalDateTime occurredAt;

}
