package cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper;

import cn.xbatis.db.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题目统计数据，目前使用单行滚动统计方案
 */
@Builder
@Data
@Table("paper_item")
@NoArgsConstructor
@AllArgsConstructor
public class PaperItemEntity{
    /**
     * 试卷主表ID (外键-PaperEntity)
     */
    private Long paperId;

    /**
     * 题号，从1开始
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
     * 本题分值
     */
    private BigDecimal score;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}