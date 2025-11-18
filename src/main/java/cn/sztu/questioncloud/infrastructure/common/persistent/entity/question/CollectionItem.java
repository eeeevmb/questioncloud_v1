package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 题集内题目，绑定版本
 */
@Builder
@Data
@Table("collection_item")
@NoArgsConstructor
@AllArgsConstructor
public class CollectionItem implements Serializable {
    /**
     * 题集ID
     */
    @TableId(IdAutoType.NONE)
    private Long collectionId;

    /**
     * 题目加入顺序，从1开始，由应用层写值
     */
    @TableId(IdAutoType.NONE)
    private Integer ordinal;

    /**
     * 题目id
     */
    private Long questionId;

    /**
     * 题目版本id
     */
    private Long questionVersionId;
}
