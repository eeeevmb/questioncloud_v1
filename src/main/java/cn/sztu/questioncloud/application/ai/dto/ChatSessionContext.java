package cn.sztu.questioncloud.application.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 聊天会话上下文
 * 用于存放业务信息
 */
@Data
@Builder
public class ChatSessionContext {
    private Long userId;

    private List<Long> collectionIds;

    private List<Long> questionIds;
}
