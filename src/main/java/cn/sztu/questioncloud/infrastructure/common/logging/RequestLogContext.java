package cn.sztu.questioncloud.infrastructure.common.logging;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RequestLogContext {
    private String traceId;
    private Long userId;
    private String method;
    private String uri;
    private String queryString;
    private String requestIp;
    private String userAgent;
}
