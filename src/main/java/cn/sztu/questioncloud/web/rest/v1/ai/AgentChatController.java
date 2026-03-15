package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.CreateChatSessionReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.OldChatSessionVO;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
public class AgentChatController {
    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * 测试聊天接口
     *
     * @param req 聊天请求
     * @return 流式响应
     */
    @PostMapping("/chatTest")
    public Flux<ServerSentEvent<String>> chatTest(@RequestBody ChatTestReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return agentService.simpleChat(userId, req)
                .map(data -> ServerSentEvent.<String>builder().data(data).event("message").build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build());
    }


    /**
     * 获取会话历史
     *
     * @param sessionId 会话ID
     * @return 会话记录
     */
    @GetMapping("/chat/{sessionId}")
    public ResultVO<List<ChatMessageVO>> getChatHistory(@PathVariable String sessionId) {
        return ResultVO.success(agentService.getChatHistory(sessionId));
    }

    @Deprecated
    @PostMapping("/collection/{collectionId}/session")
    public ResultVO<OldChatSessionVO> createNewAssistantSession(@PathVariable Long collectionId) {
        return ResultVO.success(agentService.createNewChatSession(StpUtil.getLoginIdAsLong(), collectionId));
    }
}
