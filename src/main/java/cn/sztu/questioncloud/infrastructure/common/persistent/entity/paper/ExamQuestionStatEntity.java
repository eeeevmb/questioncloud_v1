package cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试题目统计数据
 */
@Builder
@Data
@Table("exam_question_stat")
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionStatEntity{
    /**
     * 考试ID
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
     * 本题满分 (快照时的分值)
     */
    private BigDecimal maxScore;

    /**
     * 作答人次
     */
    private Integer attempts;

    /**
     * 满分人次
     */
    @TableField("full_score_cnt")
    private Integer fullScoreCount;

    /**
     * 总得分
     */
    private BigDecimal scoreSum;

    /**
     * 得分平方和 (用于计算方差)
     */
    private BigDecimal scoreSqSum;

    /**
     * 最低分
     */
    private BigDecimal minScore;

    /**
     * 实际最高分
     */
    private BigDecimal maxScoreObserved;

    /**
     * 得分分布直方图 (JSON字符串)
     */
    private String histogramJson;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
