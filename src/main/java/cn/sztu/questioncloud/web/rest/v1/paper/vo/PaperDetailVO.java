package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperEntity;
import cn.xbatis.db.annotations.Ignore;
import cn.xbatis.db.annotations.ResultEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 试卷详情视图 (含题目列表)
 * 用于：试卷详情展示、试卷预览
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaperDetailVO extends PaperBasicVO {
    @Ignore
    private List<PaperItemDetailVO> items;
}