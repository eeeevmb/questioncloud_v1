package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;
import cn.xbatis.db.annotations.ResultEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ResultEntity(QuestionCollectionEntity.class)
public class CollectionVO {
    private Long id;
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


