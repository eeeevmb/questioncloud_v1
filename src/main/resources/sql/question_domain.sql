-- 题目（不放正文）
DROP TABLE IF EXISTS question;
CREATE TABLE question (
      id                 BIGINT UNSIGNED NOT NULL COMMENT 'PK',
      status             TINYINT         NOT NULL DEFAULT 0 COMMENT '0=draft,1=active,2=archived',
      current_version_id BIGINT UNSIGNED NULL     COMMENT '当前发布版本ID（无FK，自管一致性）',
      owner_id           BIGINT UNSIGNED NULL     COMMENT '创建者',
      created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_q_status (status),
      KEY idx_q_current_ver (current_version_id),
      KEY idx_q_updated (updated_at),
      KEY idx_q_owner_status_updated (owner_id, status, updated_at)     -- 按负责人+状态+更新时间
);

-- 题目版本（正文快照；不可变语义由应用保障）
DROP TABLE IF EXISTS question_version;
CREATE TABLE question_version (
      id                   BIGINT UNSIGNED NOT NULL COMMENT 'PK',
      question_id          BIGINT UNSIGNED NOT NULL COMMENT 'question.id（无FK）',
      version_no           INT             NOT NULL COMMENT '从1递增',
      type_code            VARCHAR(32)     NOT NULL COMMENT '题型编码（应用层枚举 single-choice/multiple-choice 等）',
      title                VARCHAR(255)    NULL     COMMENT '题目标题/摘要',
      stem                 LONGTEXT        NOT NULL COMMENT '题干内容（可含 LaTeX 公式，但不含外层结构命令）',
      options              JSON            NULL     COMMENT '选择题各选项',
      answer               LONGTEXT        NULL     COMMENT '参考答案（可空）',
      answer_key           VARCHAR(64)     NULL     COMMENT '选择、填空和判断题机器判分用答案（如 A、ACD、T 等）',
      solution             LONGTEXT        NULL     COMMENT '解析内容（可含 LaTeX 公式，不含外层结构命令）',
      assets               JSON            NULL     COMMENT '图片/附件',
      created_by           BIGINT UNSIGNED NOT NULL,
      created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_qv_qid_no (question_id, version_no),
      KEY idx_qv_qid (question_id),
      KEY idx_qv_type (type_code),
      KEY idx_qv_created (created_at)
);

-- 题集
DROP TABLE IF EXISTS question_collection;
CREATE TABLE question_collection (
     id           BIGINT UNSIGNED NOT NULL COMMENT 'PK',
     name         VARCHAR(128)    NOT NULL,
     description  VARCHAR(255)    NULL,
     owner_id     BIGINT UNSIGNED NOT NULL,
     source       TINYINT         NOT NULL COMMENT '0=用户自建，1=系统默认',
     created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (id),
     UNIQUE KEY uk_qc_owner_name (owner_id, name)
);


-- 题集明细（绑定版本以保证可复现；无FK）
DROP TABLE IF EXISTS collection_item;
CREATE TABLE collection_item (
     collection_id       BIGINT UNSIGNED NOT NULL,
     ordinal             INT             NOT NULL COMMENT '顺序，从1开始',
     question_id         BIGINT UNSIGNED NOT NULL,
     question_version_id BIGINT UNSIGNED NOT NULL,
     PRIMARY KEY (collection_id, ordinal),
     KEY idx_ci_qid (question_id),
     KEY idx_ci_qvid (question_version_id),
     UNIQUE KEY uk_ci_no_dup (collection_id, question_version_id)  -- 防止题集重复同一版本
);

-- 元数据/统计读模型（滚动单行）
-- 设计要点：
-- exposure_factor 表示“last_exposed_at 时刻的基准曝光强度(0~1)”
-- 当前有效曝光值在查询/组卷时按 last_exposed_at + 衰减公式计算，不需要定时全量更新
DROP TABLE IF EXISTS question_stats;
CREATE TABLE question_stats (
        question_id      BIGINT UNSIGNED NOT NULL              COMMENT '题目ID',
        version_id       BIGINT UNSIGNED NOT NULL              COMMENT '当前版本ID（用于join当前版本）',
        attempts         INT UNSIGNED    NOT NULL DEFAULT 0    COMMENT '作答次数',
        correct_cnt      INT UNSIGNED    NOT NULL DEFAULT 0    COMMENT '正确作答次数',
        correct_rate     DECIMAL(6,4)    NULL                  COMMENT '正确率(0~1)，可由异步任务回填',
        difficulty       DECIMAL(3,2)    NULL                  COMMENT '难度系数(0~1)，人工/算法标注',
        exposure_factor  DECIMAL(3,2)    NOT NULL DEFAULT 1.00 COMMENT '曝光强度基准值(0~1)，在 last_exposed_at 时刻的值',
        last_exposed_at  DATETIME        NULL                  COMMENT '上次被组卷/下发时间（用于时间衰减）',
        updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

        PRIMARY KEY (question_id),
        KEY idx_qs_ver (version_id),
        KEY idx_qs_exposure (exposure_factor),
        KEY idx_qs_last_exposed (last_exposed_at),
        KEY idx_qs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目统计与曝光读模型';
