package cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试题目快照数据
 */
@Builder
@Data
@Table("exam_item_snapshot")
@NoArgsConstructor
@AllArgsConstructor
public class ExamItemSnapshotEntity{
    /**
     * 考试主表ID (外键-ExamEntity)
     */
    @TableId
    private Long examId;

    /**
     * 题号
     */
    private Integer seq;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 题目版本ID
     */
    private Long questionVersionId;

    /**
     * 实际分值
     */
    private BigDecimal score;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
