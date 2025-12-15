package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.common.json.TwoDecimalDoubleSerializer;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ResultEntity(QuestionEntity.class)
public class QuestionDetailVO {
    private Long id;
    private Long currentVersionId;

    @ResultEntityField(target = QuestionVersionEntity.class, property = "versionNo")
    private Integer versionNo;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "typeCode")
    private String typeCode;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "title")
    private String title;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "stem")
    private String stem;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "answer")
    private String answer;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "answerKey")
    private String answerKey;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "solution")
    private String solution;
    @ResultEntityField(target = QuestionVersionEntity.class, property = "assets")
    private List<AssetVO> assets;

    @ResultEntityField(target = QuestionStat.class, property = "attempts")
    private Integer attempts;
    @ResultEntityField(target = QuestionStat.class, property = "correctCount")
    private Integer correctCount;
    @ResultEntityField(target = QuestionStat.class, property = "correctRate")
    private Double correctRate;
    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    @ResultEntityField(target = QuestionStat.class, property = "difficulty")
    private Double difficulty;
    @JsonSerialize(using = TwoDecimalDoubleSerializer.class)
    @ResultEntityField(target = QuestionStat.class, property = "exposureFactor")
    private Double exposureFactor;
    @ResultEntityField(target = QuestionStat.class, property = "lastExposedAt")
    private LocalDateTime lastExposedAt;

    private Long ownerId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
