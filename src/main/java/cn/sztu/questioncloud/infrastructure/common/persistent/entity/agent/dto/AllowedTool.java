package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto;

import lombok.Data;

@Data
public class AllowedTool {
    private String toolName;
    private String description;
    private String paramDocs;
}