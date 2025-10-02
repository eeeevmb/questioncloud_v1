package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;


import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Table("question")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionEntity implements Serializable {
    @TableId
    private Long id;

    private Long createdBy; // 创建者ID

    /**
     * SINGLE(0),MULTI(1),JUDGE(2),FILL(3),SHORT(4),OTHER(5);
     */
    private Integer type; // 题目类型

    private String title; // 题目标题

    private String stem; // 题干

    private String options; // 选项

    private String answer;    // 答案

    private String analysis; // 解析

    /**
     * DRAFT(0),ACTIVE(1),ARCHIVED(2),DELETED(3);
     */
    private Integer status; // 题目状态

    @TableField("created_at")
    private LocalDateTime createdAt; // 创建时间

    @TableField("updated_at")
    private LocalDateTime updatedAt; // 更新时间
}