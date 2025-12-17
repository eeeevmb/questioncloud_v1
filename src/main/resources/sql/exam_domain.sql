-- paper（试卷）
DROP TABLE IF EXISTS paper;
CREATE TABLE paper (
   id           BIGINT UNSIGNED NOT NULL COMMENT 'PK',
   owner_id     BIGINT UNSIGNED NOT NULL COMMENT '创建者',
   title        VARCHAR(255)    NOT NULL,
   description  VARCHAR(255)    NULL,
   status       TINYINT         NOT NULL DEFAULT 0 COMMENT '0=draft,1=published,2=archived',

   total_items  INT UNSIGNED    NOT NULL DEFAULT 0,
   total_score  DECIMAL(10,2)   NOT NULL DEFAULT 0,

   created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
   PRIMARY KEY (id),
   KEY idx_p_owner (owner_id),
   KEY idx_p_owner_status_updated (owner_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷（模板）';


-- paper_item（试卷题目明细）
DROP TABLE IF EXISTS paper_item;
CREATE TABLE paper_item (
    paper_id            BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL COMMENT '题号，从1开始',
    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    score               DECIMAL(6,2)    NOT NULL DEFAULT 0 COMMENT '本题分值',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (paper_id, seq),
    KEY idx_pi_qid (question_id),
    KEY idx_pi_qvid (question_version_id),
    UNIQUE KEY uk_pi_no_dup (paper_id, question_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目明细（绑定题目版本）';

-- exam（一次考试实例）
DROP TABLE IF EXISTS exam;
CREATE TABLE exam (
      id            BIGINT UNSIGNED NOT NULL COMMENT 'PK',
      owner_id      BIGINT UNSIGNED NOT NULL,
      paper_id      BIGINT UNSIGNED NULL COMMENT '来源试卷，可为空（纯随机组卷也行）',

      title         VARCHAR(255)    NOT NULL,
      status        TINYINT         NOT NULL DEFAULT 0 COMMENT '0=draft,1=running,2=closed',

      total_items   INT UNSIGNED    NOT NULL DEFAULT 0,
      total_score   DECIMAL(10,2)   NOT NULL DEFAULT 0,

      stats_applied TINYINT         NOT NULL DEFAULT 0 COMMENT '0=未回灌题库统计,1=已回灌（防重复导入翻倍）',

      created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

      PRIMARY KEY (id),
      KEY idx_e_owner (owner_id),
      KEY idx_e_paper (paper_id),
      KEY idx_e_owner_status_updated (owner_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试实例';


-- exam_item_snapshot（考试题目快照）
DROP TABLE IF EXISTS exam_item_snapshot;
CREATE TABLE exam_item_snapshot (
    exam_id             BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL COMMENT '题号，从1开始',

    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    score               DECIMAL(6,2)    NOT NULL DEFAULT 0,

    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (exam_id, seq),
    KEY idx_eis_qid (question_id),
    KEY idx_eis_qvid (question_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试题目快照（固化版本与分值）';


-- exam_question_stat（按题聚合统计）
DROP TABLE IF EXISTS exam_question_stat;
CREATE TABLE exam_question_stat (
    exam_id             BIGINT UNSIGNED NOT NULL,
    seq                 INT             NOT NULL,

    question_id         BIGINT UNSIGNED NOT NULL,
    question_version_id BIGINT UNSIGNED NOT NULL,
    max_score           DECIMAL(6,2)    NOT NULL DEFAULT 0,

    attempts            INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '作答份数',
    full_score_cnt      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '满分份数',

    score_sum           DECIMAL(12,2)   NOT NULL DEFAULT 0 COMMENT '总得分',
    score_sq_sum        DECIMAL(18,4)   NOT NULL DEFAULT 0 COMMENT '平方和（可选，用于方差）',
    min_score           DECIMAL(6,2)    NULL,
    max_score_observed  DECIMAL(6,2)    NULL,

    histogram_json      JSON            NULL COMMENT '得分分布桶（可选）',

    updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (exam_id, seq),
    KEY idx_eqs_exam (exam_id),
    KEY idx_eqs_qid (question_id),
    KEY idx_eqs_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试按题聚合统计';