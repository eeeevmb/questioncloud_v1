package cn.sztu.questioncloud.infrastructure.common.logging;

import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceAccessLogWebFilter implements WebFilter {
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String TRACE_ID_ATTR = "traceId";
    private static final Logger ACCESS_LOG = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long start = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();
        String traceId = resolveTraceId(request);
        Long userId = currentUserId();
        RequestLogContext context = RequestLogContext.builder()
                .traceId(traceId)
                .userId(userId)
                .method(request.getMethod().name())
                .uri(request.getURI().getPath())
                .queryString(request.getURI().getRawQuery())
                .requestIp(resolveIp(request))
                .userAgent(request.getHeaders().getFirst(HttpHeaders.USER_AGENT))
                .build();

        exchange.getAttributes().put(TRACE_ID_ATTR, traceId);
        exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, traceId);
        MDC.put("traceId", traceId);
        if (userId != null) {
            MDC.put("userId", String.valueOf(userId));
        }
        RequestLogContextHolder.set(context);

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    int httpStatus = Optional.ofNullable(exchange.getResponse().getStatusCode())
                            .map(status -> status.value())
                            .orElse(200);
                    long costMs = System.currentTimeMillis() - start;
                    ACCESS_LOG.info(
                            "traceId={} userId={} method={} uri={} queryString={} requestIp={} userAgent=\"{}\" httpStatus={} businessCode={} costMs={}",
                            traceId,
                            userId == null ? "-" : userId,
                            context.getMethod(),
                            context.getUri(),
                            context.getQueryString() == null ? "" : context.getQueryString(),
                            context.getRequestIp(),
                            safe(context.getUserAgent()),
                            httpStatus,
                            "",
                            costMs
                    );
                    RequestLogContextHolder.clear();
                    MDC.remove("traceId");
                    MDC.remove("userId");
                });
    }

    private String resolveTraceId(ServerHttpRequest request) {
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString().replace("-", "");
        }
        return traceId.length() > 64 ? traceId.substring(0, 64) : traceId;
    }

    private Long currentUserId() {
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveIp(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String forwarded = headers.getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = headers.getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddress() == null ? "" : request.getRemoteAddress().getAddress().getHostAddress();
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}
