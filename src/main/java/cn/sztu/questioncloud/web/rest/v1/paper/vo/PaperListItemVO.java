package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.xbatis.db.annotations.ResultEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷列表项视图
 */
@Data
@ResultEntity(PaperEntity.class)
public class PaperListItemVO {
    private Long id;
    private String title;
    private Integer status;
    private Integer totalItems;
    private BigDecimal totalScore;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
}
