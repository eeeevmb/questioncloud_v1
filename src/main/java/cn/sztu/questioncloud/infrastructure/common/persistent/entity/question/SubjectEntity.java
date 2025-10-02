package cn.sztu.questioncloud.infrastructure.common.persistent.entity.question;


import cn.xbatis.db.annotations.Table;
import cn.xbatis.db.annotations.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Data
@Table("subject")
@NoArgsConstructor
@AllArgsConstructor
public class SubjectEntity {
    @TableId
    private Long id;

    @NotNull
    private String name; // 对应科目名称

    @NotNull
    private String description; // 对应科目描述

    @NotNull
    private String code; // 学科代码

    @NotNull
    private LocalDateTime createdAt; // 对应科目创建时间

    @NotNull
    private LocalDateTime updatedAt; // 对应科目更新时间

}

