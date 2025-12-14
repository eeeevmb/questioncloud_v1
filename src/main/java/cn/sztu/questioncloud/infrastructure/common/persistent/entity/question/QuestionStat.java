package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 题目统计数据，目前使用单行滚动统计方案
 */
@Builder
@Data
@Table("question_stats")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionStat {
    /**
     * 题目主表ID
     */
    @TableId
    private Long questionId;

    /**
     * 题目版本ID
     */
    private Long versionId;

    /**
     * 作答次数
     */
    private Integer attempts;

    /**
     * 正确作答次数
     */
    @TableField("correct_cnt")
    private Integer correctCount;

    /**
     * 正确率
     */
    private Double correctRate;

    /**
     * 难度，取值范围0.00~1.00，越高表示题目越难
     */
    private Double difficulty;

    /**
     * 曝光系数，变化算法依据艾宾浩斯遗忘曲线
     */
    private Double exposureFactor;

    /**
     * 上次曝光时间
     */
    private LocalDateTime lastExposedAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

}
