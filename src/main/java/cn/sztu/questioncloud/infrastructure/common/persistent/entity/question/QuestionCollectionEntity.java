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

/**
 * 题集，用于分类题目
 */
@Builder
@Data
@Table("question_collection")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCollectionEntity implements Serializable {
    /**
     * 题集主键，使用雪花算法生成
     */
    @TableId(value = IdAutoType.NONE)
    private Long id;

    /**
     * 题集名
     */
    private String name;

    /**
     * 题集简介
     */
    private String description;

    /**
     * 所有者id
     */
    private Long ownerId;

    /**
     * 来源
     * 1=系统创建，0=用户创建
     */
    private Integer source;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后修改时间
     */
    private LocalDateTime updatedAt;
}
