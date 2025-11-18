package cn.sztu.questioncloud.infrastructure.adapter.question.model;

import cn.sztu.questioncloud.infrastructure.common.file.model.InfraFileMetadata;
import cn.sztu.questioncloud.web.rest.v1.question.vo.QuestionDraftVO;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record ImportSession(
        String sessionId,
        Long userId,
        List<QuestionDraftVO> drafts,
        // slotId to file
        Map<String, InfraFileMetadata> assets
) {
}