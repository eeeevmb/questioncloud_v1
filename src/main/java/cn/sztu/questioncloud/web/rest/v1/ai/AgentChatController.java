package cn.sztu.questioncloud.web.rest.v1.ai;

import cn.dev33.satoken.stp.StpUtil;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.common.model.vo.ResultVO;
import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
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
     * 与题集小助手对话
     *
     * @param collectionId 题集ID
     * @param req          聊天请求
     * @return 流式响应
     */
    @PostMapping(value = "/collection/{collectionId}/assistant/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatWithAssistant(@PathVariable Long collectionId,
                                          @RequestBody AssistantChatReq req) {
        return agentService.chatWithAssistant(req.getMemoryId(), collectionId, req)
                .map(data -> ServerSentEvent.<String>builder().data(data).event("message").build())
                .concatWithValues(ServerSentEvent.builder("[DONE]").event("done").build());
    }

    /**
     * 获取会话历史
     *
     * @param memoryId 会话记忆ID
     * @return 会话记录
     */
    @GetMapping("/chat/{memoryId}")
    public ResultVO<List<ChatMessageVO>> getChatHistory(@PathVariable String memoryId) {
        return ResultVO.success(agentService.getChatHistory(memoryId));
    }

    /**
     * 创建新题集小助手会话
     *
     * @param collectionId 题集ID
     * @return 会话视图
     */
    @PostMapping("/collection/{collectionId}/session")
    public ResultVO<ChatSessionVO> createNewAssistantSession(@PathVariable Long collectionId) {
        return ResultVO.success(agentService.createNewChatSession(StpUtil.getLoginIdAsLong(), collectionId));
    }
}
