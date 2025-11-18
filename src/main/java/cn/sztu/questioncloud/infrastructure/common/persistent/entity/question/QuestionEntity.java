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
 * 题目表实体，存储题目元信息
 */

@Builder
@Data
@Table("question")
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEntity implements Serializable {
    /**
     * 题目主键，使用雪花算法生成
     */
    @TableId(value = IdAutoType.NONE)
    private Long id;

    /**
     * 题目状态（0=draft,1=active,2=archived）
     */
    private Integer status;

    /**
     * 最新题目版本id
     */
    private Long currentVersionId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 最后修改时间
     */
    private LocalDateTime updatedAt;
}
