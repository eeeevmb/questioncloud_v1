-- updated_at更新时由应用层写值
-- 应用层用雪花算法写入主键id

DROP TABLE IF EXISTS question_type;
CREATE TABLE question_type (
       code              VARCHAR(32)  NOT NULL COMMENT '题型代码：single_choice/fill_blank等',
       name              VARCHAR(64)  NOT NULL COMMENT '显示名',
       status            TINYINT      NOT NULL DEFAULT 1 COMMENT '1=active,0=inactive',
       created_at        DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
       updated_at        DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY (code),
       KEY idx_qtype_status (status)
);

-- 2) 题目（聚合根；不放正文）
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

-- 3) 题目版本（正文快照；不可变语义由应用保障）
DROP TABLE IF EXISTS question_version;
CREATE TABLE question_version (
      id                   BIGINT UNSIGNED NOT NULL COMMENT 'PK',
      question_id          BIGINT UNSIGNED NOT NULL COMMENT 'question.id（无FK）',
      version_no           INT             NOT NULL COMMENT '从1递增',
      type_code            VARCHAR(32)     NOT NULL COMMENT 'question_type.code（无FK）',
      title                VARCHAR(255)    NULL     COMMENT '题目标题/摘要',
      stem                 LONGTEXT        NOT NULL COMMENT '题干 LaTeX',
      options              JSON            NULL     COMMENT '选项（选择题）',
      answer               JSON            NULL     COMMENT '答案结构（含主观题）',
      solution             LONGTEXT        NULL     COMMENT '解析 LaTeX',
      assets               JSON            NULL     COMMENT '图片/附件',
      created_by           BIGINT UNSIGNED NOT NULL,
      created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_qv_qid_no (question_id, version_no),
      KEY idx_qv_qid (question_id),
      KEY idx_qv_type (type_code),
      KEY idx_qv_created (created_at)
);

-- 4) 题集
DROP TABLE IF EXISTS question_collection;
CREATE TABLE question_collection (
     id           BIGINT UNSIGNED NOT NULL COMMENT 'PK',
     name         VARCHAR(128)    NOT NULL,
     description  VARCHAR(255)    NULL,
     owner_id     BIGINT UNSIGNED NULL,
     created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (id),
     KEY idx_qc_owner (owner_id)
);


-- 5) 题集明细（绑定版本以保证可复现；无FK）
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

-- 6) 元数据/统计读模型（异步作业写入；可用于排序/推荐/报表）
DROP TABLE IF EXISTS question_stats;
CREATE TABLE question_stats (
       question_id     BIGINT UNSIGNED NOT NULL,
       version_id      BIGINT UNSIGNED NOT NULL,
       attempts        INT             NOT NULL DEFAULT 0 COMMENT '作答次数',
       correct_cnt     INT             NOT NULL DEFAULT 0 COMMENT '正确作答次数',
       correct_rate    DECIMAL(6,4)    NULL               COMMENT '正确率',
       difficulty      DECIMAL(3,2)    NULL               COMMENT '难度系数',
       exposure_factor DECIMAL(3,2)    NULL               COMMENT '曝光系数',
       window_start    DATETIME        NULL,
       window_end      DATETIME        NULL,
       updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
       PRIMARY KEY (question_id, version_id),
       KEY idx_qm_ver (version_id),
       KEY idx_qm_exposure (exposure_factor),
       KEY idx_qm_updated (updated_at),
       KEY idx_qm_qid (question_id)
);
