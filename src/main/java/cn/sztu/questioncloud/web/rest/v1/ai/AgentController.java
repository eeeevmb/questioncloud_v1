package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.service.AgentChatService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.CreateChatSessionReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class AgentController {
    private final AgentChatService agentChatService;

    @PostMapping("/chat")
    public Flux<ServerSentEvent<String>> chat(@Valid @RequestBody AssistantChatReq req) {
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

    @PostMapping
    public ResultVO<ChatSessionVO> createNewSession(@Valid @RequestBody CreateChatSessionReq req) {
        Long userId = StpUtil.getLoginIdAsLong();
        return ResultVO.success(agentChatService.createNewChatSession(userId, req.getAgentName()));
    }

    // 获取所有可用agent
}
