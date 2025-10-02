package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;

import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableField;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
@Table("knowledge_point")
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgePointEntity {

    @TableId
    private Long id;

    @NotNull
    private String name;// 知识点名称

    private String description;// 知识点描述

    @NotNull
    private String source_type;//知识点提取来源

    private String example;// 知识点示例

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private LocalDateTime updatedAt;
}
