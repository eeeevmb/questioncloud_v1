package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolExecutionRequest {
    private String id;
    private String name;
    private Map<String, Object> arguments;
}
