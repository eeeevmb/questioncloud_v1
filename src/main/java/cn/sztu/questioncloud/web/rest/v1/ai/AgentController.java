package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.service.AgentChatService;
import cn.sztu.questioncloud.application.ai.service.AiGenerateService;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.*;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.AgentPaperDraftVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final AgentChatService agentChatService;
    private final AiGenerateService aiGenerateService;

    @PostMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody ChatReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        ChatSessionContext context = ChatSessionContext.builder()
                .userId(userId)
                .collectionIds(req.getContext().getCollectionIds())
                .questionIds(req.getContext().getSelectedQuestionIds())
                .build();
        return agentChatService.chat(context, req.getSessionId(), req.getAgentName(), req.getMessage())
                .map(data -> ServerSentEvent.<String>builder().data(data).event("message").build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build());
    }

    /**
     * 创建新聊天会话
     * @param req 创建会话请求
     * @return 响应
     */
    @PostMapping
    public ResultVO<ChatSessionVO> createNewSession(@Valid @RequestBody CreateChatSessionReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(agentChatService.createNewChatSession(userId, req.getAgentName()));
    }

    /**
     * 获取当前用户聊天会话列表
     * @return 响应
     */
    @GetMapping
    public ResultVO<List<ChatSessionVO>> getChatSessions() {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(agentChatService.getChatSessions(userId));
    }

    /**
     * 更新聊天会话标题
     *
     * @param sessionId 会话ID
     * @param req 更新请求
     * @return 响应
     */
    @PutMapping("/sessions/{sessionId}/title")
    public ResultVO<Void> updateChatSessionTitle(@PathVariable Long sessionId,
                                                 @Valid @RequestBody UpdateChatSessionTitleReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentChatService.updateChatSessionTitle(userId, sessionId, req.getTitle());
        return ResultVO.success();
    }

    /**
     * 删除单个聊天会话
     *
     * @param sessionId 会话ID
     * @return 响应
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResultVO<Void> deleteChatSession(@PathVariable Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        agentChatService.deleteChatSession(userId, sessionId);
        return ResultVO.success();
    }

    /**
     * 获取聊天会话历史
     * @param sessionId 会话ID
     * @return 响应
     */
    @GetMapping("/sessions/{sessionId}/message")
    public ResultVO<List<ChatMessageVO>> getChatMessage(@PathVariable Long sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(agentChatService.getChatMessages(userId, sessionId));
    }

    /**
     * 根据用户描述生成题目草稿
     * @return 题目草稿
     */
    @PostMapping("/question-draft/generate")
    public ResultVO<QuestionDraft> generateQuestionDraft(@Valid @RequestBody GenerateQuestionDraftReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(aiGenerateService.generateQuestionDraft(req.toString()));
    }

    /**
     * 根据用户描述生成候选题目清单
     */
    @PostMapping("/paper-draft/generate")
    public ResultVO<AgentPaperDraftVO> generateAgentPaperDraft(@Valid @RequestBody GeneratePaperDraftReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(aiGenerateService.generateAgentPaperDraft(userId, req));
    }
}
