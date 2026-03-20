package cn.sztu.questioncloud.infrastructure.common.persistent.entity.rag;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题目与知识点关联实体
 * 核心：建立题目与知识点的高质量映射集
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("question_knowledge_rel")
public class KnowledgeQuestionRelEntity implements Serializable {
    /**
     * 关联记录ID (主键)
     */
    @TableId(value = IdAutoType.AUTO)
    private Long id;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 知识点ID
     */
    private Long knowledgePointId;

    /**
     * 是否为该题的核心主考点 (true: 核心考点, false: 只是次要涉及)
     */
    private Boolean isMain;

    /**
     * 相关度分数 (0-100，AI打分，便于后期 RAG 检索时做排序惩罚/奖励)
     */
    private Integer relevanceScore;

    /**
     * 关联来源 (例如: MANUAL-人工添加, AI_EXTRACTED-AI自动提取)
     */
    private Integer sourceType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}