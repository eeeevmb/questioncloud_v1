package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.port.CollectionAssistantChatPort;
import cn.sztu.questioncloud.infrastructure.common.ai.service.CollectionAssistantAiService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AiChatAdapter implements CollectionAssistantChatPort {
    private final CollectionAssistantAiService aiService;

    public AiChatAdapter(CollectionAssistantAiService aiService) {
        this.aiService = aiService;
    }


    /**
     * 与大模型进行对话
     *
     * @param memoryId 会话记忆ID
     * @param message  用户消息
     * @return 流式响应
     */
    @Override
    public Flux<String> chat(String memoryId, String message) {

        return aiService.chatTest(memoryId, message);
    }
}
