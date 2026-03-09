package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.SessionMetadata;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.SessionMetadataTypeHandler;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Table("chat_session")
public class ChatSessionEntity {
    @TableId
    private Long id;

    private Long agentId;

    private Long userId;

    private String title;

    @TableField(value = "metadata", typeHandler = SessionMetadataTypeHandler.class)
    private SessionMetadata metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
