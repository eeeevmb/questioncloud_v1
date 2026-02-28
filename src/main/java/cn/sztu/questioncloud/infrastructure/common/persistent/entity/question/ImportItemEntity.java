package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.sztu.questioncloud.application.importer.dto.ImportErrorReport;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.ImportErrorListJsonTypeHandler;
import cn.sztu.questioncloud.infrastructure.common.persistent.handler.QuestionDraftJsonTypeHandler;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Table("import_item")
@NoArgsConstructor
@AllArgsConstructor
public class ImportItemEntity {
    @TableId
    private Long id;

    @TableField("import_id")
    private Long importId;

    @TableField("collection_name")
    private String collectionName;

    @TableField("index_no")
    private Integer indexNo;

    @TableField(value = "draft", typeHandler = QuestionDraftJsonTypeHandler.class)
    private QuestionDraft draft;

    /**
     * 0=VALID, 1=INVALID
     */
    private Integer status;

    @TableField(value = "errors", typeHandler = ImportErrorListJsonTypeHandler.class)
    private List<ImportErrorReport> errors;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
