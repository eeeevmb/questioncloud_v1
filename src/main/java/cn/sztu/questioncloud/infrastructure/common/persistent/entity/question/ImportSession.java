package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table("import_session")
public class ImportSession {
    @TableId
    private Long id;

    @TableField("user_id")
    private Long userId;

    /**
     * 0=PARSING,1=READY,2=COMMITTING,3=COMMITTED,4=CANCELED,5=FAILED
     */
    private Integer status;

    @TableField("file_id")
    private Long fileId;

    /**
     * 0=EXCEL
     */
    private Integer format;

    private Integer total;

    @TableField("valid_cnt")
    private Integer validCnt;

    @TableField("invalid_cnt")
    private Integer invalidCnt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    private Integer version;
}
