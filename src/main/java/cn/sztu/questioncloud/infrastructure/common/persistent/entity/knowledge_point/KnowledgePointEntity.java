package cn.sztu.questioncloud.infrastructure.common.persistent.entity.knowledge_point;

import cn.sztu.questioncloud.infrastructure.common.persistent.handler.StringListTypeHandler;
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识点实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("knowledge_point")
public class KnowledgePointEntity implements Serializable {

    /**
     * 知识点ID
     */
    @TableId(value = IdAutoType.AUTO)
    private Long id;

    /**
     * 所属知识点空间
     */
    @NotNull
    private Long knowledgeScopeId;

    /**
     * 标准知识点名称
     */
    private String canonicalName;

    /**
     * 知识点描述
     */
    private String description;

    /**
     * 别名列表
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> aliases;

    /**
     * 公式或代码
     */
    private String formulaOrCode;

    /**
     * 示例
     */
    private String example;

    /**
     * 来源
     */
    @NotNull
    private Integer sourceType;

    /**
     * 创建时间
     */
    @NotNull
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @NotNull
    private LocalDateTime updatedAt;

}
