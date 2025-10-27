package cn.sztu.questioncloud.application.user.messaging;

import java.time.LocalDateTime;

/**
 * 用户注册消息载荷
 *
 * @param userId     新用户ID
 * @param occurredAt 事件发生时间
 * @author Codex
 */
public record UserRegisteredMessage(
        Long userId,
        LocalDateTime occurredAt) {}
