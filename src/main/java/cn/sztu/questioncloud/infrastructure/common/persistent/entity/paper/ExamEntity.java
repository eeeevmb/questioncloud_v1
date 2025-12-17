package cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 考试表实体，存储题目元信息
 */
@Builder
@Data
@Table("exam")
@NoArgsConstructor
@AllArgsConstructor
public class ExamEntity implements Serializable {
    /**
     * 考试实例主键，使用雪花算法生成
     */
    @TableId(value = IdAutoType.NONE)
    private Long id;

    /**
     * 负责人/监考人ID
     */
    private Long ownerId;

    /**
     * 来源试卷ID (可为空)
     */
    private Long paperId;

    /**
     * 考试标题
     */
    private String title;

    /**
     * 状态 (0=draft, 1=running, 2=closed)
     */
    private Integer status;

    /**
     * 题目总数
     */
    private Integer totalItems;

    /**
     * 卷面总分
     */
    private BigDecimal totalScore;

    /**
     * 统计状态 (0=未回灌题库统计,1=已回灌,防重复导入翻倍)
     */
    private Integer statsApplied;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
