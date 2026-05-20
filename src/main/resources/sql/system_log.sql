-- 系统操作日志
-- 用于记录关键用户行为，支撑首页 Dashboard 系统动态、审计和问题追踪。
CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT UNSIGNED NOT NULL COMMENT '日志ID，雪花ID',
    user_id BIGINT UNSIGNED NULL COMMENT '操作用户ID，系统行为可为空',
    username VARCHAR(64) NULL COMMENT '操作用户名快照',
    module VARCHAR(64) NOT NULL COMMENT '业务模块，如 QUESTION/COLLECTION/PAPER/IMPORT/AI/USER',
    action VARCHAR(64) NOT NULL COMMENT '操作动作，如 CREATE/UPDATE/DELETE/IMPORT_COMMIT/AI_GENERATE',
    target_type VARCHAR(64) NULL COMMENT '目标资源类型，如 QUESTION/COLLECTION/PAPER/IMPORT_SESSION',
    target_id BIGINT UNSIGNED NULL COMMENT '目标资源ID',
    target_name VARCHAR(255) NULL COMMENT '目标资源名称快照',
    content VARCHAR(512) NOT NULL COMMENT '用于前端展示的动态文案',
    request_method VARCHAR(16) NULL COMMENT '请求方法',
    request_uri VARCHAR(255) NULL COMMENT '请求URI',
    request_ip VARCHAR(64) NULL COMMENT '请求IP',
    user_agent VARCHAR(512) NULL COMMENT 'User-Agent',
    trace_id VARCHAR(64) NULL COMMENT '链路追踪ID',
    result VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '结果：SUCCESS/FAIL',
    error_message VARCHAR(512) NULL COMMENT '失败原因摘要',
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为实际发生时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志入库时间',

    PRIMARY KEY (id),
    KEY idx_sol_user_time (user_id, occurred_at),
    KEY idx_sol_user_result_time (user_id, result, occurred_at),
    KEY idx_sol_module_action_time (module, action, occurred_at),
    KEY idx_sol_target (target_type, target_id),
    KEY idx_sol_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志';
