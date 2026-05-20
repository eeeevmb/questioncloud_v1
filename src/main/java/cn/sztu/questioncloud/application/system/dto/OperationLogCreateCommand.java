package cn.sztu.questioncloud.application.system.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OperationLogCreateCommand {
    private Long userId;
    private String username;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String content;
    private String requestMethod;
    private String requestUri;
    private String requestIp;
    private String userAgent;
    private String traceId;
    private String result;
    private String errorMessage;
    private LocalDateTime occurredAt;
}
