package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
public class AgentChatController {
    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chatTest")
    public Flux<String> chatTest(@RequestBody ChatTestReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return agentService.simpleChat(userId, req);
    }

    @PostMapping(value = "/collection/{collectionId}/assistant/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatWithAssistant(@PathVariable Long collectionId,
                                          @RequestBody AssistantChatReq req) {
        return agentService.chatWithAssistant(req.getMemoryId(), collectionId, req.getMessage());
    }

    @PostMapping("/collection/{collectionId}/session")
    public ResultVO<ChatSessionVO> createNewSession(@PathVariable Long collectionId) {
        return ResultVO.success(agentService.createNewChatSession(StpUtil.getLoginIdAsLong(), collectionId));
    }
}
