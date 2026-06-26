package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
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
@Table("question_scope_rel")
public class QuestionScopeRelEntity implements Serializable {

    @TableId(value = IdAutoType.NONE)
    private Long id;

    private Long questionId;

    private Long knowledgeScopeId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
