package cn.sztu.questioncloud.infrastructure.common.persistent.entity.rag;

import cn.sztu.questioncloud.infrastructure.common.persistent.handler.StringListTypeHandler; // 注意替换为你项目实际的Handler路径
import cn.xbatis.db.IdAutoType;
import cn.xbatis.db.annotations.LogicDelete;
import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
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
@Table("knowledge_points")
public class KnowledgePointEntity implements Serializable {
    /**
     * 知识点ID (主键)
     */
    @TableId(value = IdAutoType.AUTO)
    private Long id;

    private String title;

    /**
     * 知识点核心定义/描述
     */
    private String description;

    /**
     * 向量数据缓存
     */
    private String embeddingVector;

    /**
     * 别名集合
     */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> aliases;

    /**
     * 核心公式或代码段
     */
    private String formulaOrCode;

    /**
     * 典型示例
     */
    private String example;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    //-------- 以下为可拓展字段，尚缺乏使用思路，实际开发中暂时不使用 --------

    /**
     * 考点重要度/权重 ： 暂时由AI评估生成，与实际情况误差较大，实际开发中暂不使用
     */
    private Integer importanceWeight;

    /**
     * 父级知识点ID (用于构建树形目录)
     */
    private Long parentId;

    /**
     * 所属文件夹/分类ID
     */
    private Long folderId;
}