package cn.sztu.questioncloud.web.rest.v1.question.vo;

import lombok.Builder;

import java.util.List;

@Builder
public record QuestionDraftVO(
        Long draftId,
        String title,
        String stemLatex,
        String solutionLatex,
        List<AssetVO> assets) {

    public static QuestionDraftVO of(Long draftId,
                                     String title,
                                     String stemLatex,
                                     String solutionLatex,
                                     List<AssetVO> assets) {
        return QuestionDraftVO.builder()
                .draftId(draftId)
                .title(title)
                .stemLatex(stemLatex)
                .solutionLatex(solutionLatex)
                .assets(assets)
                .build();
    }
}
