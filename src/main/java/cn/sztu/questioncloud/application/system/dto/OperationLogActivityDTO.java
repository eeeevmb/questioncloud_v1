package cn.sztu.questioncloud.application.system.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OperationLogActivityDTO {
    private String type;
    private String username;
    private String content;
    private LocalDateTime occurredAt;
    private String timeText;
}
