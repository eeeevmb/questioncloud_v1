DROP TABLE IF EXISTS infra_file_metadata;
CREATE TABLE infra_file_metadata
(
    fm_id                BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文件唯一ID (自增生成)',
    fm_storage_path      VARCHAR(512) NOT NULL COMMENT '文件在物理存储中的相对路径',
    fm_owner_identifier  VARCHAR(128) NOT NULL COMMENT '文件所有者的唯一标识',
    fm_access_level      SMALLINT     NOT NULL DEFAULT 1 COMMENT '访问级别代码 (0: PRIVATE, 1: PUBLIC)',
    fm_original_filename VARCHAR(255) NOT NULL COMMENT '原始文件名',
    fm_file_size         BIGINT       NOT NULL COMMENT '文件大小（字节）',
    fm_mime_type         VARCHAR(128) COMMENT 'MIME类型',
    fm_provider          VARCHAR(64)  NOT NULL COMMENT '存储提供商（例如：local, oss）',
    fm_created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    fm_grantees          JSON         DEFAULT NULL COMMENT '被授权访问该文件的用户标识列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';

CREATE INDEX idx_infra_file_metadata_owner ON infra_file_metadata (fm_owner_identifier);
CREATE INDEX idx_infra_file_metadata_access_level ON infra_file_metadata (fm_access_level);
CREATE INDEX idx_infra_file_metadata_provider ON infra_file_metadata (fm_provider);
CREATE INDEX idx_infra_file_metadata_created_at ON infra_file_metadata (fm_created_at);