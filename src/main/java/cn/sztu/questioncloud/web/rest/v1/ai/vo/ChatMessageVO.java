package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.Content;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionRequest;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageVO {
    /**
     * ChatMessageType 的字符串表示，如 SYSTEM/USER/AI/TOOL_EXECUTION_RESULT/CUSTOM。
     */
    private String type;

    /**
     * 与前端约定的角色标识（system/user/assistant/tool/custom）。
     */
    private String role;

    /**
     * 主要文本内容，适用于 System/Ai/ToolExecutionResult 等消息。
     */
    private String text;

    /**
     * LLM 的思维链（如果可用）。
     */
    private String thinking;

    /**
     * 用户消息的内容片段（文本、图片、音频等）。
     */
    private List<Content> contents;

    /**
     * AI 消息中携带的工具调用请求。
     */
    private List<ToolExecutionRequest> toolExecutionRequests;

    /**
     * 工具执行结果。
     */
    private ToolExecutionResult toolExecutionResult;

    /**
     * 附加属性，来自 AiMessage/CustomMessage 的 attributes。
     */
    private Map<String, Object> attributes;


}
