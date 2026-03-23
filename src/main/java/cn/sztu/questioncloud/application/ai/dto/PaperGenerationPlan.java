package cn.sztu.questioncloud.application.ai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM返回的组卷计划
 */
@Data
public class PaperGenerationPlan {
    /**
     * LLM 生成的选题原因
     */
    private String reason;

    /**
     * 候选题目桶
     */
    private List<BucketPlan> bucketPlans;

    @Data
    public static class BucketPlan {
        private String typeCode;
        private List<String> topics;
    }

    public Map<String, List<String>> getTypeCodeToTopic() {
        return this.getBucketPlans().stream()
                .collect(Collectors.toMap(
                        BucketPlan::getTypeCode,
                        BucketPlan::getTopics
                ));
    }
}
