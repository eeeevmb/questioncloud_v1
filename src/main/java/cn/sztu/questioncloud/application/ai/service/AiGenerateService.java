package cn.sztu.questioncloud.application.ai.service;

import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.web.rest.v1.ai.req.GeneratePaperDraftReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.AgentPaperDraftVO;

public interface AiGenerateService {
    /**
     * 根据用户描述生成题目草稿
     *
     * @param userInput 用户描述
     * @return 题目草稿
     */
    QuestionDraft generateQuestionDraft(String userInput);

    /**
     * 生成Agent组卷草稿
     * @param userId 用户ID
     * @param req    组卷请求
     * @return 试卷草稿视图
     */
    AgentPaperDraftVO generateAgentPaperDraft(Long userId, GeneratePaperDraftReq req);
}
