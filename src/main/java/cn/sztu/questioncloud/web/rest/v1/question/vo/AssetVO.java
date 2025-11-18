package cn.sztu.questioncloud.web.rest.v1.question.vo;

import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetSection;
import cn.sztu.questioncloud.infrastructure.common.persistent.enums.AssetType;
import lombok.Builder;

@Builder
public record AssetVO(
        String slotId,
        AssetSection section,
        Integer ordinal,
        AssetType assetType,
        String originalLatex,
        String latexPlaceholder,
        String suggestedFilename) {

    public static AssetVO of(String slotId,
                             AssetSection section,
                             int ordinal,
                             AssetType assetType,
                             String originalLatex,
                             String latexPlaceholder,
                             String suggestedFilename) {
        return AssetVO.builder()
                .slotId(slotId)
                .section(section)
                .ordinal(ordinal)
                .assetType(assetType)
                .originalLatex(originalLatex)
                .latexPlaceholder(latexPlaceholder)
                .suggestedFilename(suggestedFilename)
                .build();
    }
}
