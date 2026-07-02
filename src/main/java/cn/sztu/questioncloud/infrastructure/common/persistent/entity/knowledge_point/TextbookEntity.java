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
@Table("textbook")
public class TextbookEntity implements Serializable {

    @TableId(value = IdAutoType.NONE)
    private Long id;

    @NotNull
    private Long knowledgeScopeId;

    @NotNull
    private String textbookName;

    private String edition;

    private String author;

    private String publisher;

    private String isbn;

    private Long createdBy;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;
}
