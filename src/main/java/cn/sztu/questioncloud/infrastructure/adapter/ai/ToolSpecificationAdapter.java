package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.port.ToolSpecificationPort;
import cn.sztu.questioncloud.infrastructure.common.ai.spec.PaperToolSpecificationFactory;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.AllowedTool;
import dev.langchain4j.agent.tool.ToolSpecification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ToolSpecificationAdapter implements ToolSpecificationPort {
    private final PaperToolSpecificationFactory paperToolSpecificationFactory;

    public ToolSpecificationAdapter(PaperToolSpecificationFactory paperToolSpecificationFactory) {
        this.paperToolSpecificationFactory = paperToolSpecificationFactory;
    }

    @Override
    public List<ToolSpecification> getToolSpecifications(List<AllowedTool> allowedTools) {
        Set<String> toolNames = allowedTools.stream().map(AllowedTool::getToolName).collect(Collectors.toSet());

        return toolNames.stream()
                .map(name -> paperToolSpecificationFactory.manualSpecifications().get(name))
                .toList();
    }

}
