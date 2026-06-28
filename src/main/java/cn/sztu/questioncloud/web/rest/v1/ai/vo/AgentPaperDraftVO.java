package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Agent生成的试卷草稿
 */
@Data
@Builder
public class AgentPaperDraftVO {
    /**
     * 智能体的选题原因
     */
    private String reason;

    /**
     * 候选题目清单
     */
    private List<CandidateQuestionVO> candidateQuestions;
}
