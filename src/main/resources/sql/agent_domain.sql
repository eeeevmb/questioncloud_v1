DROP TABLE IF EXISTS agent;
CREATE TABLE agent (
        id                 BIGINT UNSIGNED NOT NULL COMMENT 'PK',
        name               VARCHAR(255)    NOT NULL COMMENT '智能体名称',
        description        VARCHAR(255)    NULL     COMMENT '(用户可见)智能体介绍',
        status             TINYINT         NOT NULL DEFAULT 0 COMMENT '0=disable, 1=active',
        system_prompt      TEXT            NULL     COMMENT '系统提示词',
        allowed_tools      JSON            NULL     COMMENT '可用工具列表',
        allowed_kbs        JSON            NULL     COMMENT '可用知识库列表',
        chat_options       JSON            NULL     COMMENT '配置项(模型名、温度、top_p、最大token)',
        created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id),
        UNIQUE KEY uk_name (name),
        KEY idx_status_updated (status, updated_at)
) COMMENT='智能体模版表';

DROP TABLE IF EXISTS chat_session;
CREATE TABLE chat_session (
        id                 BIGINT UNSIGNED NOT NULL COMMENT 'PK',
        agent_id           BIGINT UNSIGNED NOT NULL COMMENT '绑定的智能体ID',
        user_id            BIGINT UNSIGNED NULL     COMMENT '用户ID',
        title              VARCHAR(255)    NULL     COMMENT '智能体自动生成的标题',
        metadata           JSON            NULL     COMMENT '扩展(输入语言、设备类型)',
        created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id)
) COMMENT='聊天会话';

DROP TABLE IF EXISTS chat_message;
CREATE TABLE chat_message (
        id                 BIGINT UNSIGNED NOT NULL COMMENT 'PK',
        session_id         BIGINT UNSIGNED NOT NULL COMMENT '会话ID',
        type               VARCHAR(32)     NOT NULL COMMENT 'SYSTEM,USER,AI,TOOL_EXECUTION_RESULT,CUSTOM',
        role               VARCHAR(32)     NOT NULL COMMENT 'system,user,assistant,tool,custom',
        text_content       TEXT            NULL     COMMENT '文本内容',
        thinking_content   TEXT            NULL     COMMENT 'LLM思维链',
        contents_json      JSON            NULL     COMMENT '用户消息的内容片段',
        tool_req_json      JSON            NULL     COMMENT '工具调用请求',
        tool_result_json   JSON            NULL     COMMENT '工具执行结果',
        attributes_json    JSON            NULL     COMMENT '附加属性',
        created_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        updated_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (id)
) COMMENT='聊天消息';

