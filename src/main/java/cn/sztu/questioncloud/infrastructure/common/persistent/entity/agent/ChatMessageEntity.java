package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.Content;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionRequest;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionResult;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.ChatMessageContentListTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.JsonMapTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.ToolExecutionRequestListTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.ToolExecutionResultTypeHandler;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Table("chat_message")
public class ChatMessageEntity {
    @TableId
    private Long id;

    private Long sessionId;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 与前端约定展示的角色
     */
    private String role;

    private String textContent;

    private String thinkingContent;

    /**
     * 用户消息的内容片段
     */
    @TableField(value = "contents_json", typeHandler = ChatMessageContentListTypeHandler.class)
    private List<Content> content;

    @TableField(value = "tool_req_json", typeHandler = ToolExecutionRequestListTypeHandler.class)
    private List<ToolExecutionRequest> toolExecutionRequest;

    @TableField(value = "tool_result_json", typeHandler = ToolExecutionResultTypeHandler.class)
    private ToolExecutionResult toolExecutionResult;

    /**
     * 附加属性，来自 AiMessage/CustomMessage 的 attributes。
     */
    @TableField(value = "attributes_json", typeHandler = JsonMapTypeHandler.class)
    private Map<String, Object> attributes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
