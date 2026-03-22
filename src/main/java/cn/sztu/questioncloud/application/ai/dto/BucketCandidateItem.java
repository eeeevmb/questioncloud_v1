package cn.sztu.questioncloud.application.ai.dto;

import lombok.Data;

@Data
public class BucketCandidateItem {
    private QuestionHitDTO question;
    private int hitCount;
    private int bestMatchRank;
}