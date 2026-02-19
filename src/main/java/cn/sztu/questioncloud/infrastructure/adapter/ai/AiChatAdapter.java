package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.port.CollectionAssistantChatPort;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.ai.service.CollectionAssistantAiService;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class AiChatAdapter implements CollectionAssistantChatPort {
    private final CollectionAssistantAiService aiService;
    private final CacheService cacheService;

    public AiChatAdapter(CollectionAssistantAiService aiService, CacheService cacheService) {
        this.aiService = aiService;
        this.cacheService = cacheService;
    }

    /**
     * 与大模型进行对话
     *
     * @param memoryId 会话记忆ID
     * @param message  用户消息
     * @return 流式响应
     */
    @Override
    public Flux<String> chatTest(String memoryId, String message) {
        return aiService.chatTest(memoryId, message);
    }

    /**
     * 与题集小助手进行对话
     *
     * @param memoryId     会话记忆ID
     * @param collectionId 题集ID
     * @param message      用户消息
     * @return 流式响应
     */
    @Override
    public Flux<String> chatWithAssistant(String memoryId, Long collectionId, String message) {
        ChatSessionContext context = cacheService.get(CacheKeyUtil.chatContextKey(memoryId));

        List<Long> questionIds = context.getQuestionIds();
        String ctx;
        if (questionIds == null || questionIds.isEmpty()) {
            ctx = "用户暂未选定题目";
        } else {
            ctx = "用户已选定" + questionIds.size() + "道题目，调用查看题目详情工具以查看";
        }

        return aiService.chatWithAssistant(memoryId, collectionId, ctx, message);
    }
}
