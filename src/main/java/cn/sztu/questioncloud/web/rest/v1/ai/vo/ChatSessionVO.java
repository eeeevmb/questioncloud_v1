package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.SessionMetadata;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionVO {
    private Long sessionId;

    private String agentName;

    private Long userId;

    private String title;

    private SessionMetadata metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
