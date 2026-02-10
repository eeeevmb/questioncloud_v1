package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.SessionVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai")
public class AgentChatController {
    private final AgentService agentService;

    public AgentChatController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public Flux<String> chatTest(@RequestBody ChatReq req) {
        return agentService.chatTest(req.getMemoryId(), req.getMessage());
    }

    @PostMapping("/session")
    public ResultVO<SessionVO> createNewSession() {
        return ResultVO.success(agentService.createNewSession());
    }
}
