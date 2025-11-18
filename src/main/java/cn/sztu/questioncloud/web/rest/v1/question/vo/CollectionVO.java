package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionCollectionEntity;

public record CollectionVO(
        String name,
        String description
) {
    public static CollectionVO fromEntity(QuestionCollectionEntity entity) {
        return new CollectionVO(
                entity.getName(),
                entity.getDescription()
        );
    }
}
