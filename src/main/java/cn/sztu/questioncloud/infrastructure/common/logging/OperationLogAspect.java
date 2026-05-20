package cn.sztu.questioncloud.infrastructure.common.logging;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.system.dto.OperationLogCreateCommand;
import cn.sztu.questioncloud.application.system.enums.OperationLogResultEnum;
import cn.sztu.questioncloud.application.system.service.OperationLogService;
import cn.sztu.questioncloud.application.user.port.UserAccountRepository;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.user.UserAccountEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogService operationLogService;
    private final UserAccountRepository userAccountRepository;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        LocalDateTime occurredAt = LocalDateTime.now();
        try {
            Object result = joinPoint.proceed();
            record(joinPoint, operationLog, result, null, OperationLogResultEnum.SUCCESS, occurredAt);
            return result;
        } catch (Throwable ex) {
            if (operationLog.recordFailure()) {
                record(joinPoint, operationLog, null, ex, OperationLogResultEnum.FAIL, occurredAt);
            }
            throw ex;
        } finally {
            OperationLogSupport.clear();
        }
    }

    private void record(ProceedingJoinPoint joinPoint,
                        OperationLog operationLog,
                        Object result,
                        Throwable throwable,
                        OperationLogResultEnum logResult,
                        LocalDateTime occurredAt) {
        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                    joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
            context.setVariable("result", result);
            context.setVariable("error", throwable);
            for (Map.Entry<String, Object> entry : OperationLogSupport.snapshot().entrySet()) {
                context.setVariable(entry.getKey(), entry.getValue());
            }

            Long userId = resolveUserId(operationLog, context);
            String targetName = evalString(operationLog.targetName(), context);
            context.setVariable("targetName", targetName);
            Long targetId = evalLong(operationLog.targetId(), context);
            String content = evalString(operationLog.content(), context);
            RequestLogContext requestContext = RequestLogContextHolder.get();
            String username = resolveUsername(userId);

            operationLogService.record(OperationLogCreateCommand.builder()
                    .userId(userId)
                    .username(username)
                    .module(operationLog.module())
                    .action(operationLog.action())
                    .targetType(blankToNull(operationLog.targetType()))
                    .targetId(targetId)
                    .targetName(targetName)
                    .content(content)
                    .requestMethod(requestContext == null ? null : requestContext.getMethod())
                    .requestUri(requestContext == null ? null : requestContext.getUri())
                    .requestIp(requestContext == null ? null : requestContext.getRequestIp())
                    .userAgent(requestContext == null ? null : requestContext.getUserAgent())
                    .traceId(requestContext == null ? null : requestContext.getTraceId())
                    .result(logResult.getCode())
                    .errorMessage(throwable == null ? null : throwable.getMessage())
                    .occurredAt(occurredAt)
                    .build());
        } catch (Exception e) {
            log.warn("操作日志切面记录失败: method={}, message={}", joinPoint.getSignature().toShortString(), e.getMessage());
        }
    }

    private Long resolveUserId(OperationLog operationLog, MethodBasedEvaluationContext context) {
        Long userId = evalLong(operationLog.userId(), context);
        if (userId != null) {
            return userId;
        }
        RequestLogContext requestContext = RequestLogContextHolder.get();
        if (requestContext != null && requestContext.getUserId() != null) {
            return requestContext.getUserId();
        }
        try {
            return StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveUsername(Long userId) {
        if (userId == null) {
            return null;
        }
        return userAccountRepository.findById(userId)
                .map(UserAccountEntity::getUsername)
                .orElse(null);
    }

    private String evalString(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        return value == null ? null : String.valueOf(value);
    }

    private Long evalLong(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
