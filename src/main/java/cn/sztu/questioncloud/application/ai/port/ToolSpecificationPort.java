package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedTool;
import dev.langchain4j.agent.tool.ToolSpecification;

import java.util.List;

/**
 * 工具调用接口
 */
public interface ToolSpecificationPort {

    List<ToolSpecification> getToolSpecifications(List<AllowedTool> allowedTools);
}
