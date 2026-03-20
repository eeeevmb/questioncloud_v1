package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.List;

public interface ToolExecutionPort {
    // 执行工具
    List<ToolExecutionResultMessage> executeTool(Long sessionId,
                                                 List<ToolExecutionRequest> requests,
                                                 AgentDefinition definition);
}
