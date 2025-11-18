package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.dto.AssetSnapshot;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.question.QuestionVersionEntity;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record QuestionVO(
        Long id,
        Long currentVersionId,
        Integer versionNO,
        String typeCode,
        String title,
        String stem,
        String answer,
        String solution,
        List<AssetSnapshot> assets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt

) {
    public static QuestionVO fromEntity(QuestionEntity entity, QuestionVersionEntity versionEntity) {
        return QuestionVO.builder()
                .id(entity.getId())
                .currentVersionId(entity.getCurrentVersionId())
                .versionNO(versionEntity.getVersionNo())
                .title(versionEntity.getTitle())
                .stem(versionEntity.getStem())
                .answer(versionEntity.getAnswer())
                .solution(versionEntity.getSolution())
                .assets(versionEntity.getAssets())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
