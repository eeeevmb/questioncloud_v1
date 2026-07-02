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
@Table("knowledge_scope")
public class KnowledgeScopeEntity implements Serializable {

    @TableId(value = IdAutoType.NONE)
    private Long id;

    @NotNull
    private String scopeName;

    private String description;

    private Long createdBy;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;

    @NotNull
    private Integer isDeleted;
}
