package cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("textbook_knowledge_rel")
public class TextbookKnowledgeRelEntity implements Serializable {

    @TableId(value = IdAutoType.NONE)
    private Long id;

    @NotNull
    private Long textbookId;

    @NotNull
    private Long knowledgePointId;

    @NotNull
    private Integer sourceType;

    @NotNull
    private Long createdBy;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;

    @NotNull
    private Integer isDeleted;
}
