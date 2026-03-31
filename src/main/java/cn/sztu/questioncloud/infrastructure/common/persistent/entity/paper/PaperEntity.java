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
 * 试卷表实体，存储题目元信息
 */
@Builder
@Data
@Table("paper")
@NoArgsConstructor
@AllArgsConstructor
public class PaperEntity implements Serializable {
    /**
     * 试卷主键，使用雪花算法生成
     */
    @TableId(value = IdAutoType.NONE)
    private Long id;

    /**
     * 创建者ID
     */
    private Long ownerId;

    /**
     * 试卷标题
     */
    private String title;

    /**
     * 试卷描述
     */
    private String description;

    /**
     * 题目总数
     */
    private Integer totalItems;

    /**
     * 卷面总分
     */
    private BigDecimal totalScore;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
