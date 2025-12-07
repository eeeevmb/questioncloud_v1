package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionStat;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ResultEntity(QuestionEntity.class)
public class QuestionSummaryVO {
        private Long id;

        private Long currentVersionId;

        @ResultEntityField(target = QuestionVersionEntity.class, property = "versionNo")
        private Integer versionNo;

        @ResultEntityField(target = QuestionVersionEntity.class, property = "typeCode")
        private String typeCode;

        @ResultEntityField(target = QuestionVersionEntity.class, property = "title")
        private String title;

        @ResultEntityField(target = QuestionStat.class, property = "difficulty")
        private Double difficulty;

        @ResultEntityField(target = QuestionStat.class, property = "correctRate")
        private Double correctRate;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;
}
