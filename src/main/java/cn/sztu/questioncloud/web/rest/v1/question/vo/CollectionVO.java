package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import cn.xbatis.db.annotations.ResultEntity;
import cn.xbatis.db.annotations.ResultEntityField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ResultEntity(QuestionCollectionEntity.class)
public class CollectionVO {
    @ResultEntityField(property = "id")
    private Long collectionId;
    private String name;
    private String description;

    public static CollectionVO fromEntity(QuestionCollectionEntity entity) {
        return new CollectionVO(
                entity.getId(),
                entity.getName(),
                entity.getDescription()
        );
    }
}


