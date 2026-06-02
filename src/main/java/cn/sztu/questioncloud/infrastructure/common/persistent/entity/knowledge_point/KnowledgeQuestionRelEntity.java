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

/**
 * 题目知识点关联实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_question_rel")
public class KnowledgeQuestionRelEntity implements Serializable {
    /**
     * 关联ID
     */
    @TableId(value = IdAutoType.AUTO)
    private Long id;

    /**
     * 题目ID
     */
    @NotNull
    private Long questionId;

    /**
     * 题目版本ID
     */
    @NotNull
    private Long questionVersionId;

    /**
     * 知识点ID
     */
    @NotNull
    private Long knowledgePointId;

    /**
     * 是否主要考点
     * 0=主要考点，1=次要考点
     */
    @NotNull
    private Integer isMain;

    /**
     * 相关度
     */
    private Integer relevanceScore;

    /**
     * 来源
     * 0=人工，1=AI
     */
    @NotNull
    private Integer sourceType;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @NotNull
    private LocalDateTime updatedAt;
}