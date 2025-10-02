package cn.sztu.questioncloud.infrastructure.common.persistent.entity.bank;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Table("question_bank")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionBankEntity {
    // TODO 后续考虑添加题目总数，现在暂时用SQL实时计算
    @TableId
    private Long id; // 题库ID

    private String name; // 题库名称

    private String description; // 题库描述

    @TableField("owner_user_id")
    private Long ownerId; // 题库所有者ID

    /**
     * ACTIVE(0), INACTIVE(1);
     */
    private Integer status; // 状态（如：活跃，停用等）

    /**
     * PRIVATE(0), PUBLIC(1), ORG(2);
     */
    private Integer visibility; // 可见性（如：公开、私有、组织共享等）

    private LocalDateTime createdAt; // 创建时间

    private LocalDateTime updatedAt; // 更新时间


}

