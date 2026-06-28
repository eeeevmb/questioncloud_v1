package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionResult;

/**
 * chat_message.tool_result_json JSON 列与 ToolExecutionResult 之间转换。
 */
public class ToolExecutionResultTypeHandler extends JacksonTypeHandler<ToolExecutionResult> {

    public ToolExecutionResultTypeHandler() {
        super(ToolExecutionResult.class);
    }
}
