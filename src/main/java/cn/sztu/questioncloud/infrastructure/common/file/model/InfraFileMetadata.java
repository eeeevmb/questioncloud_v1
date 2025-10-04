package cn.sztu.questioncloud.infrastructure.common.file.model;


import cn.sztu.questioncloud.infrastructure.common.persistent.handler.StringListTypeHandler;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件元数据实体
 *
 * @author LinSanQi
 */
@Data
@Table(value = "infra_file_metadata")
@NoArgsConstructor
public class InfraFileMetadata implements Serializable {
    @Serial
    @Ignore
    private static final long serialVersionUID = 1L;
    /**
     * 文件唯一ID (自增生成)
     */
    @TableId(value = IdAutoType.AUTO)
    private Long fmId;
    /**
     * 文件在物理存储中的相对路径
     */
    private String fmStoragePath;
    /**
     * 文件所有者的唯一标识
     */
    private String fmOwnerIdentifier;
    /**
     * 访问级别代码
     *
     * @see cn.sztu.questioncloud.infrastructure.common.file.enums.AccessLevel
     */
    private Integer fmAccessLevel;
    /**
     * 被授权访问该文件的用户标识列表
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> fmGrantees = new ArrayList<>();
    /**
     * 原始文件名
     */
    private String fmOriginalFilename;
    /**
     * 文件大小（字节）
     */
    private Long fmFileSize;
    /**
     * MIME类型
     */
    private String fmMimeType;
    /**
     * 存储提供商（例如：local, oss）
     */
    private String fmProvider;
    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fmCreatedAt;

    public InfraFileMetadata(String storagePath, String ownerIdentifier, Integer accessLevel, String originalFilename, Long fileSize, String mimeType, String provider) {
        this.fmStoragePath = storagePath;
        this.fmOwnerIdentifier = ownerIdentifier;
        this.fmAccessLevel = accessLevel;
        this.fmOriginalFilename = originalFilename;
        this.fmFileSize = fileSize;
        this.fmMimeType = mimeType;
        this.fmProvider = provider;
        this.fmCreatedAt = LocalDateTime.now();
    }

    public InfraFileMetadata(Long fmId, String storagePath, String ownerIdentifier, Integer accessLevel, String originalFilename, Long fileSize, String mimeType, String provider) {
        this.fmId = fmId;
        this.fmStoragePath = storagePath;
        this.fmOwnerIdentifier = ownerIdentifier;
        this.fmAccessLevel = accessLevel;
        this.fmOriginalFilename = originalFilename;
        this.fmFileSize = fileSize;
        this.fmMimeType = mimeType;
        this.fmProvider = provider;
        this.fmCreatedAt = LocalDateTime.now();
    }
}
