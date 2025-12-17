package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情视图 (含题目列表)
 * 用于：试卷详情展示、试卷预览
 */
@Data
@ResultEntity(PaperEntity.class)
public class PaperDetailVO {
    private Long id;
    // --- 基础信息 ---
    private String title;
    private String description;
    private Integer status;

    // --- 统计信息 ---
    private Integer totalItems;
    private BigDecimal totalScore;

    // --- 试题信息 ---
    @Ignore
    private List<PaperItemVO> items;

    // --- 其他信息 ---
    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}