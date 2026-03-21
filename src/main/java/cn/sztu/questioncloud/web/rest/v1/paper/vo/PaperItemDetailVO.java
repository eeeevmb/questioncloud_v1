package cn.sztu.questioncloud.web.rest.v1.paper.vo;

import cn.sztu.questioncloud.common.json.TwoDecimalDoubleSerializer;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.paper.PaperItemEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.dto.QuestionOption;
import cn.sztu.questioncloud.web.rest.v1.question.vo.AssetVO;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ResultEntity(PaperItemEntity.class) // 映射到关联表
public class PaperItemDetailVO {
    // --- 基础信息 ---
    private Long paperId;
    private Long questionId;

    // --- 试题信息 ---
    private BigDecimal score;
    private Integer seq;

    // --- 题目信息 ---
    @ResultEntityField(target = QuestionVersionEntity.class, property = "typeCode")
    private String typeCode;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "title")
    private String questionTitle;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "stem")
    private String stem;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "options")
    private List<QuestionOption> options;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "assets")
    private List<AssetVO> assets;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "versionNo")
    private Integer versionNo;

    // --- 题目统计信息(辅助判断) ---
    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    @ResultEntityField(target = QuestionStat.class, property = "difficulty")
    private Double difficulty;
    @ResultEntityField(target = QuestionStat.class, property = "correctRate")
    private Double correctRate;
}
