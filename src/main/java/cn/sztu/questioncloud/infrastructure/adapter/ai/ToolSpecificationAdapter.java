package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.port.ToolSpecificationPort;
import cn.sztu.questioncloud.infrastructure.common.ai.tool.QuestionTool;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ToolSpecificationAdapter implements ToolSpecificationPort {
    private final List<ToolSpecification> availableTools;

    public ToolSpecificationAdapter() {
        // 后续还实现了其他工具就继续拼接
        this.availableTools = ToolSpecifications.toolSpecificationsFrom(QuestionTool.class);
    }

    @Override
    public List<ToolSpecification> getToolSpecifications(List<AllowedTool> allowedTools) {
        Set<String> toolNames = allowedTools.stream().map(AllowedTool::getToolName).collect(Collectors.toSet());
        return availableTools.stream()
                .filter(toolSpecification -> toolNames.contains(toolSpecification.name()))
                .toList();
    }
}
