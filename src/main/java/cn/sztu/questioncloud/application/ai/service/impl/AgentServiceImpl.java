package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.helper.ChatMessageConverter;
import cn.sztu.questioncloud.application.ai.port.CollectionAssistantChatPort;
import cn.sztu.questioncloud.application.ai.service.AgentService;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.web.rest.v1.ai.req.AssistantChatReq;
import cn.sztu.questioncloud.web.rest.v1.ai.req.ChatTestReq;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
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
        refreshSelectedQuestions(req.getMemoryId(),
                Optional.ofNullable(req.getContext()).map(ChatTestReq.Context::getSelectedQuestionIds).orElse(null));
        return collectionAssistantChatPort.chatTest(req.getMemoryId(), req.getMessage());
    }

    /**
     * 与题集小助手对话
     *
     * @param memoryId     会话记忆ID
     * @param collectionId 题集ID
     * @param req          聊天请求
     * @return 流式响应
     */
    public Flux<String> chatWithAssistant(String memoryId, Long collectionId, AssistantChatReq req) {
        refreshSelectedQuestions(memoryId,
                Optional.ofNullable(req.getContext()).map(AssistantChatReq.Context::getSelectedQuestionIds).orElse(null));
        return collectionAssistantChatPort.chatWithAssistant(memoryId, collectionId, req.getMessage());
    }

    /**
     * 获取会话历史
     *
     * @param memoryId 记忆ID
     * @return 会话历史
     */
    @Override
    public List<ChatMessageVO> getChatHistory(String memoryId) {
        String json = cacheService.get(CacheKeyUtil.chatMemoryKey(memoryId));
        if (json == null || json.isBlank()) {
            return List.of();
        }
        List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(json);
        return ChatMessageConverter.toVOList(messages);
    }

    /**
     * 创建新会话
     *
     * @return 会话视图对象
     */
    @Override
    public ChatSessionVO createNewChatSession(Long userId, Long collectionId) {
        // 1. 生成会话记忆ID
        String memoryId = UUID.randomUUID().toString();

        // 2. 将业务上下文信息写入Redis
        String key = CacheKeyUtil.chatContextKey(memoryId);
        ChatSessionContext context = ChatSessionContext.builder()
                .userId(userId)
                .collectionId(collectionId)
                .build();

        cacheService.set(key, context, MEMORY_TTL_DAYS, TimeUnit.DAYS);

        // 3. 构造结果返回
        return ChatSessionVO.builder()
                .memoryId(memoryId)
                .collectionId(collectionId)
                .build();
    }

    /**
     * 刷新所选题目
     *
     * @param memoryId 会话记忆ID
     * @param candidateIds 候选ID
     */
    private void refreshSelectedQuestions(String memoryId, List<Long> candidateIds) {
        String key = CacheKeyUtil.chatContextKey(memoryId);
        ChatSessionContext context = cacheService.get(key);
        if (context == null) {
            log.warn("上下文不存在，memoryId={}", memoryId);
            return;
        }

        List<Long> newIds = candidateIds == null ? List.of() : candidateIds;
        List<Long> oldIds = Optional.ofNullable(context.getQuestionIds()).orElse(List.of());

        if (!Objects.equals(oldIds, newIds)) {
            context.setQuestionIds(newIds);
        }
        cacheService.set(key, context, MEMORY_TTL_DAYS, TimeUnit.DAYS);
    }
}
