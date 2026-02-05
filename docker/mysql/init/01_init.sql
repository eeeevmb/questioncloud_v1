CREATE DATABASE IF NOT EXISTS questioncloud_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE questioncloud_db;

-- ===== question_domain.sql =====
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
      KEY idx_q_owner_status_updated (owner_id, status, updated_at)
);

DROP TABLE IF EXISTS question_version;
CREATE TABLE question_version (
      id                   BIGINT UNSIGNED NOT NULL COMMENT 'PK',
      question_id          BIGINT UNSIGNED NOT NULL COMMENT 'question.id（无FK）',
      version_no           INT             NOT NULL COMMENT '从1递增',
      type_code            VARCHAR(32)     NOT NULL COMMENT '题型编码',
      title                VARCHAR(255)    NULL     COMMENT '题目标题/摘要',
      stem                 LONGTEXT        NOT NULL COMMENT '题干内容',
      options              JSON            NULL     COMMENT '选择题各选项',
      answer               LONGTEXT        NULL     COMMENT '参考答案（可空）',
      answer_key           VARCHAR(64)     NULL     COMMENT '机器判分答案',
      solution             LONGTEXT        NULL     COMMENT '解析内容',
      assets               JSON            NULL     COMMENT '图片/附件',
      created_by           BIGINT UNSIGNED NOT NULL,
      created_at           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      UNIQUE KEY uk_qv_qid_no (question_id, version_no),
      KEY idx_qv_qid (question_id),
      KEY idx_qv_type (type_code),
      KEY idx_qv_created (created_at)
);

DROP TABLE IF EXISTS question_collection;
CREATE TABLE question_collection (
     id           BIGINT UNSIGNED NOT NULL COMMENT 'PK',
     name         VARCHAR(128)    NOT NULL,
     description  VARCHAR(255)    NULL,
     owner_id     BIGINT UNSIGNED NULL,
     source       TINYINT         NOT NULL COMMENT '0=用户自建，1=系统默认',
     created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
     PRIMARY KEY (id),
     KEY idx_qc_owner (owner_id)
);

DROP TABLE IF EXISTS collection_item;
CREATE TABLE collection_item (
     collection_id       BIGINT UNSIGNED NOT NULL,
     ordinal             INT             NOT NULL COMMENT '顺序，从1开始',
     question_id         BIGINT UNSIGNED NOT NULL,
     question_version_id BIGINT UNSIGNED NOT NULL,
     PRIMARY KEY (collection_id, ordinal),
     KEY idx_ci_qid (question_id),
     KEY idx_ci_qvid (question_version_id),
     UNIQUE KEY uk_ci_no_dup (collection_id, question_version_id)
);

DROP TABLE IF EXISTS question_stats;
CREATE TABLE question_stats (
        question_id      BIGINT UNSIGNED NOT NULL,
        version_id       BIGINT UNSIGNED NOT NULL,
        attempts         INT UNSIGNED    NOT NULL DEFAULT 0,
        correct_cnt      INT UNSIGNED    NOT NULL DEFAULT 0,
        correct_rate     DECIMAL(6,4)    NULL,
        difficulty       DECIMAL(3,2)    NULL,
        exposure_factor  DECIMAL(3,2)    NOT NULL DEFAULT 1.00,
        last_exposed_at  DATETIME        NULL,
        updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
        PRIMARY KEY (question_id),
        KEY idx_qs_ver (version_id),
        KEY idx_qs_exposure (exposure_factor),
        KEY idx_qs_last_exposed (last_exposed_at),
        KEY idx_qs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目统计与曝光读模型';

-- ===== exam_domain.sql =====
DROP TABLE IF EXISTS paper;
CREATE TABLE paper (
   id           BIGINT UNSIGNED NOT NULL COMMENT 'PK',
   owner_id     BIGINT UNSIGNED NOT NULL,
   title        VARCHAR(255)    NOT NULL,
   description  VARCHAR(255)    NULL,
   status       TINYINT         NOT NULL DEFAULT 0,
   total_items  INT UNSIGNED    NOT NULL DEFAULT 0,
   total_score  DECIMAL(10,2)   NOT NULL DEFAULT 0,
   created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (id),
   KEY idx_p_owner (owner_id),
   KEY idx_p_owner_status_updated (owner_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷（模板）';

DROP TABLE IF EXISTS paper_item;
CREATE TABLE paper_item (
    paper_id            BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL,
    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    score               DECIMAL(6,2)    NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (paper_id, seq),
    KEY idx_pi_qid (question_id),
    KEY idx_pi_qvid (question_version_id),
    UNIQUE KEY uk_pi_no_dup (paper_id, question_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目明细';

DROP TABLE IF EXISTS exam;
CREATE TABLE exam (
      id            BIGINT UNSIGNED NOT NULL,
      owner_id      BIGINT UNSIGNED NOT NULL,
      paper_id      BIGINT UNSIGNED NULL,
      title         VARCHAR(255)    NOT NULL,
      status        TINYINT         NOT NULL DEFAULT 0,
      total_items   INT UNSIGNED    NOT NULL DEFAULT 0,
      total_score   DECIMAL(10,2)   NOT NULL DEFAULT 0,
      stats_applied TINYINT         NOT NULL DEFAULT 0,
      created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (id),
      KEY idx_e_owner (owner_id),
      KEY idx_e_paper (paper_id),
      KEY idx_e_owner_status_updated (owner_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试实例';

DROP TABLE IF EXISTS exam_item_snapshot;
CREATE TABLE exam_item_snapshot (
    exam_id             BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL,
    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    score               DECIMAL(6,2)    NOT NULL DEFAULT 0,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (exam_id, seq),
    KEY idx_eis_qid (question_id),
    KEY idx_eis_qvid (question_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题目快照';

DROP TABLE IF EXISTS exam_question_stat;
CREATE TABLE exam_question_stat (
    exam_id             BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL,
    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    max_score           DECIMAL(6,2)    NOT NULL DEFAULT 0,
    attempts            INT UNSIGNED    NOT NULL DEFAULT 0,
    full_score_cnt      INT UNSIGNED    NOT NULL DEFAULT 0,
    score_sum           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    score_sq_sum        DECIMAL(18,4)   NOT NULL DEFAULT 0,
    min_score           DECIMAL(6,2)    NULL,
    max_score_observed  DECIMAL(6,2)    NULL,
    histogram_json      JSON            NULL,
    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (exam_id, seq),
    KEY idx_eqs_exam (exam_id),
    KEY idx_eqs_qid (question_id),
    KEY idx_eqs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试按题聚合统计';

-- ===== infra_file.sql =====
DROP TABLE IF EXISTS infra_file_metadata;
CREATE TABLE infra_file_metadata
(
    fm_id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    fm_storage_path      VARCHAR(512) NOT NULL,
    fm_owner_identifier  VARCHAR(128) NOT NULL,
    fm_access_level      SMALLINT     NOT NULL DEFAULT 1,
    fm_original_filename VARCHAR(255) NOT NULL,
    fm_file_size         BIGINT       NOT NULL,
    fm_mime_type         VARCHAR(128),
    fm_provider          VARCHAR(64)  NOT NULL,
    fm_created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fm_grantees          JSON         DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

CREATE INDEX idx_infra_file_metadata_owner ON infra_file_metadata (fm_owner_identifier);
CREATE INDEX idx_infra_file_metadata_access_level ON infra_file_metadata (fm_access_level);
CREATE INDEX idx_infra_file_metadata_provider ON infra_file_metadata (fm_provider);
CREATE INDEX idx_infra_file_metadata_created_at ON infra_file_metadata (fm_created_at);

-- ===== user.sql =====
DROP TABLE IF EXISTS user;
CREATE TABLE `user`
(
    `id` bigint NOT NULL COMMENT '用户ID',
    `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    `email` varchar(255) DEFAULT NULL,
    `phone_number` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
    `avatar_url` varchar(255) DEFAULT NULL,
    `status` tinyint NOT NULL DEFAULT '1',
    `failed_login_count` int NOT NULL DEFAULT '0',
    `locked_until` datetime DEFAULT NULL,
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `username` (`username`) USING BTREE
);
