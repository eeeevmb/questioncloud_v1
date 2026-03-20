package cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Content {
    private String type;
    private String text;
    private String url;
    private String mimeType;
    private String detailLevel;
}