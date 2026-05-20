package cn.sztu.questioncloud.infrastructure.common.persistent.entity.system;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("sys_operation_log")
public class SysOperationLogEntity {
    @TableId(value = IdAutoType.NONE)
    private Long id;

    @TableField("user_id")
    private Long userId;

    private String username;

    private String module;

    private String action;

    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    @TableField("target_name")
    private String targetName;

    private String content;

    @TableField("request_method")
    private String requestMethod;

    @TableField("request_uri")
    private String requestUri;

    @TableField("request_ip")
    private String requestIp;

    @TableField("user_agent")
    private String userAgent;

    @TableField("trace_id")
    private String traceId;

    private String result;

    @TableField("error_message")
    private String errorMessage;

    @TableField("occurred_at")
    private LocalDateTime occurredAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
