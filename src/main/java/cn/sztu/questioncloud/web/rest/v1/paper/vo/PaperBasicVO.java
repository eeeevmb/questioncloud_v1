package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷基础信息视图 (不含题目列表)
 * 用于：试卷列表展示、编辑元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ResultEntity(PaperEntity.class) // 直接映射 PaperEntity
public class PaperBasicVO {
    private Long id;
    // --- 基础信息 ---
    private String title;
    private String description;

    // --- 统计信息 ---
    private Integer totalItems;
    private BigDecimal totalScore;

    // --- 其他信息 ---
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}