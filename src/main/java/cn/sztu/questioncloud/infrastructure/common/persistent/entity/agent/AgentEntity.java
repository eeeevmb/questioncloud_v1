package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedKnowledgeBase;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedTool;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ChatOptions;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.AllowedKnowledgeBaseListTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.AllowedToolListTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.ChatOptionsTypeHandler;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Table("agent")
public class AgentEntity {
    @TableId
    private Long id;

    private String name;

    private String description;

    private Integer status;

    private String systemPrompt;

    @TableField(value = "allowed_tools", typeHandler = AllowedToolListTypeHandler.class)
    private List<AllowedTool> allowedTools;

    @TableField(value = "allowed_kbs", typeHandler = AllowedKnowledgeBaseListTypeHandler.class)
    private List<AllowedKnowledgeBase> allowedKnowledgeBases;

    @TableField(value = "chat_options", typeHandler = ChatOptionsTypeHandler.class)
    private ChatOptions chatOptions;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
