package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionResult {
    private String id;
    private String toolName;
    private String text;
}
