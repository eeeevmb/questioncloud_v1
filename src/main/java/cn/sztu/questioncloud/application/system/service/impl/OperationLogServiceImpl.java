package cn.sztu.questioncloud.application.system.service.impl;

import cn.sztu.questioncloud.application.system.dto.OperationLogActivityDTO;
import cn.sztu.questioncloud.application.system.dto.OperationLogCreateCommand;
import cn.sztu.questioncloud.application.system.enums.OperationLogResultEnum;
import cn.sztu.questioncloud.application.system.port.OperationLogRepository;
import cn.sztu.questioncloud.application.system.service.OperationLogService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.system.SysOperationLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {
    private final OperationLogRepository operationLogRepository;

    @Override
    public void record(OperationLogCreateCommand command) {
        if (command == null || isBlank(command.getModule()) || isBlank(command.getAction()) || isBlank(command.getContent())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        SysOperationLogEntity entity = SysOperationLogEntity.builder()
                .id(HutoolSnowflakeIdGenerator.generateLongId())
                .userId(command.getUserId())
                .username(limit(command.getUsername(), 64))
                .module(limit(command.getModule(), 64))
                .action(limit(command.getAction(), 64))
                .targetType(limit(command.getTargetType(), 64))
                .targetId(command.getTargetId())
                .targetName(limit(command.getTargetName(), 255))
                .content(limit(command.getContent(), 512))
                .requestMethod(limit(command.getRequestMethod(), 16))
                .requestUri(limit(command.getRequestUri(), 255))
                .requestIp(limit(command.getRequestIp(), 64))
                .userAgent(limit(command.getUserAgent(), 512))
                .traceId(limit(command.getTraceId(), 64))
                .result(isBlank(command.getResult()) ? OperationLogResultEnum.SUCCESS.getCode() : limit(command.getResult(), 32))
                .errorMessage(limit(command.getErrorMessage(), 512))
                .occurredAt(command.getOccurredAt() == null ? now : command.getOccurredAt())
                .createdAt(now)
                .build();
        try {
            operationLogRepository.save(entity);
        } catch (Exception e) {
            log.warn("系统操作日志入库失败: module={}, action={}, targetId={}, message={}",
                    entity.getModule(), entity.getAction(), entity.getTargetId(), e.getMessage());
        }
    }

    @Override
    public List<OperationLogActivityDTO> listRecentActivities(Long userId, int limit) {
        int size = limit <= 0 ? 5 : Math.min(limit, 20);
        return operationLogRepository.listRecentSuccessActivities(userId, size)
                .stream()
                .map(entity -> OperationLogActivityDTO.builder()
                        .type(entity.getModule() + "_" + entity.getAction())
                        .username(entity.getUsername())
                        .content(entity.getContent())
                        .occurredAt(entity.getOccurredAt())
                        .timeText(toTimeText(entity.getOccurredAt()))
                        .build())
                .toList();
    }

    private String toTimeText(LocalDateTime occurredAt) {
        if (occurredAt == null) {
            return "";
        }
        Duration duration = Duration.between(occurredAt, LocalDateTime.now());
        if (duration.toMinutes() < 1) {
            return "刚刚";
        }
        if (duration.toHours() < 1) {
            return duration.toMinutes() + "分钟前";
        }
        if (duration.toDays() < 1) {
            return duration.toHours() + "小时前";
        }
        return duration.toDays() + "天前";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
