package cn.sztu.questioncloud.web.rest.v1.question.vo;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Builder
public record ParseResultVO(
        String importSessionId,
        List<QuestionDraftVO> questionDrafts
) { public static ParseResultVO of(String importSessionId, List<QuestionDraftVO> drafts) {
        return ParseResultVO.builder()
                .importSessionId(importSessionId)
                .questionDrafts(drafts)
                .build();
    }
}
