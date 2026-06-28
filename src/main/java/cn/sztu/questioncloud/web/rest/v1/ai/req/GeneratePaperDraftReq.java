package cn.sztu.questioncloud.web.rest.v1.ai.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GeneratePaperDraftReq {
    /**
     * 组卷描述
     */
    private String message;

    /**
     * 选题范围
     */
    private List<Long> collectionIds;

    /**
     * 选题约束
     */
    private List<BucketConstrain> constrains;

    @Data
    public static class BucketConstrain {
        @NotNull(message = "题目类型不能为空")
        private String typeCode;

        @NotNull(message = "题目数量不能为空")
        private Integer count;   // 例如: 5道

        private Double difficultyMin;

        private Double difficultyMax;
    }
}
