package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.port.CollectionAssistantChatPort;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class AgentServiceImpl implements AgentService {
    private final CollectionAssistantChatPort collectionAssistantChatPort;
    private final CacheService cacheService;

    private static final long MEMORY_TTL_DAYS = 3L;

    public AgentServiceImpl(CollectionAssistantChatPort collectionAssistantChatPort, CacheService cacheService) {
        this.collectionAssistantChatPort = collectionAssistantChatPort;
        this.cacheService = cacheService;
    }

    /**
     * 测试用聊天接口
     *
     * @param userId 用户ID
     * @param req    聊天请求
     * @return 流式响应
     */
    public Flux<String> simpleChat(Long userId, ChatTestReq req) {
        // 1. 更新Redis中的上下文内容
        ChatSessionContext context = cacheService.get(req.getMemoryId());

        List<Long> oldIds = Optional.ofNullable(context.getQuestionIds()).orElse(List.of());
        List<Long> newIds = Optional.ofNullable(req.getContext().getSelectedQuestionIds()).orElse(List.of());

        if (!Objects.equals(oldIds, newIds)) {
            context.setQuestionIds(newIds);
        }
        // 重置TTL
        cacheService.set(req.getMemoryId(), context, MEMORY_TTL_DAYS, TimeUnit.DAYS);

        // 2. 生成回复
        return collectionAssistantChatPort.chatTest(req.getMemoryId(), req.getMessage());
    }

    /**
     * 与题集小助手对话
     *
     * @param memoryId     会话记忆ID
     * @param collectionId 题集ID
     * @param message      用户消息
     * @return 流式响应
     */
    @Override
    public Flux<String> chatWithAssistant(String memoryId, Long collectionId, String message) {
        return collectionAssistantChatPort.chatWithAssistant(memoryId, collectionId, message);
    }

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    @Override
    public ChatSessionVO createNewChatSession(Long userId, Long collectionId) {
        // 1. 生成会话记忆ID
        String memoryId = HutoolSnowflakeIdGenerator.generateId();

        // 2. 将业务上下文信息写入Redis
        ChatSessionContext context = ChatSessionContext.builder()
                .userId(userId)
                .collectionId(collectionId)
                .build();

        cacheService.set(memoryId, context, MEMORY_TTL_DAYS, TimeUnit.DAYS);

        // 3. 构造结果返回
        return ChatSessionVO.builder()
                .memoryId(memoryId)
                .collectionId(collectionId)
                .build();
    }
}
