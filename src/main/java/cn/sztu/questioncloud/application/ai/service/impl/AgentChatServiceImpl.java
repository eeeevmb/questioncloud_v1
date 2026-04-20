package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.enums.AgentErrorCodeEnum;
import cn.sztu.questioncloud.application.ai.helper.ChatMessageConverter;
import cn.sztu.questioncloud.application.ai.port.*;
import cn.sztu.questioncloud.common.constant.enums.result.impl.CommonResultCodeEnum;
import cn.sztu.questioncloud.application.ai.service.AgentChatService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.ai.memory.RedisChatMemoryStore;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatSessionEntity;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.hibernate.cache.spi.support.CacheUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentChatServiceImpl implements AgentChatService {
    private final AgentDefinitionPort agentDefinitionPort;
    private final ToolSpecificationPort toolSpecificationPort;
    private final LlmPort llmPort;
    private final ToolExecutionPort toolExecutionPort;
    private final RedisChatMemoryStore chatMemoryStore;
    private final ChatSessionContextPort chatSessionContextPort;
    private final ChatSessionRepository chatSessionRepository;
    private final AgentRepository agentRepository;
    private final CacheService cacheService;

    private static final Integer MAX_STEP = 20;
    private static final String THINK_MODULE_AGENT_NAME = "ThinkModule";
    private static final String AGENT_NAME_ID_MAP_CACHE_KEY = "agent:map:name-id";

    /**
     * Agent 对话执行主流程。
     * <p>
     * 流程说明：
     * 1) 校验会话与智能体定义，写入会话上下文；
     * 2) 组装消息并进入 think-execute 循环，按需调用工具；
     * 3) 当本轮不再请求工具后，切换到流式输出最终回复；
     * 4) 结束时清理工具协议消息并回写到记忆存储。
     *
     * @param context   会话业务上下文（题集、已选题等）
     * @param sessionId 会话ID
     * @param agentName 智能体名称
     * @param userInput 用户输入
     * @return 最终回复的流式文本分片
     */
    @Override
    public Flux<String> chat(ChatSessionContext context, Long sessionId, String agentName, String userInput) {
        ChatSessionEntity sessionEntity = chatSessionRepository.findById(sessionId);
        if (sessionEntity == null) {
            throw new ApplicationException(AgentErrorCodeEnum.AGENT_CHAT_SESSION_NOT_FOUND);
        }
        // 写入业务上下文供下游读取
        chatSessionContextPort.saveContext(sessionId, context);
        // 获取Agent定义
        AgentDefinition definition = agentDefinitionPort.findByName(agentName);
        AgentDefinition thinkDefinition = agentDefinitionPort.findByName(THINK_MODULE_AGENT_NAME);
        if (definition == null || thinkDefinition == null) {
            throw new ApplicationException(AgentErrorCodeEnum.AGENT_NOT_FOUND);
        }
        // 获取当前Agent的工具介绍
        List<ToolSpecification> toolSpecifications = toolSpecificationPort.getToolSpecifications(definition.getAllowedTools());


        // 构建聊天记录
        List<ChatMessage> messages = chatMemoryStore.getMessages(sessionId);
        if (messages == null || messages.isEmpty()) {
            // 首次聊天，同时生成会话标题
            messages = new ArrayList<>();
            String title = llmPort.generateSessionTitle(new UserMessage(userInput));
            sessionEntity.setTitle(title);
            chatSessionRepository.update(sessionEntity);
        }

        // 更新系统提示词
        if (!messages.isEmpty()) {
            messages.removeFirst();
        }
        messages.addFirst(new SystemMessage(definition.getSystemPrompt().replace("{{ctx}}", renderContext(context))));
        messages.add(new UserMessage(userInput));
        chatMemoryStore.updateMessages(sessionId, messages);

        // 开始think-execute循环
        boolean finished = false;
        for (int i = 0; i < MAX_STEP; i++) {
            // 思考是否需要调用工具
            List<ChatMessage> thinkMessages = buildThinkMessages(messages, thinkDefinition, context);
            ChatResponse response = llmPort.chat(thinkDefinition, thinkMessages, toolSpecifications);
            // 检查是否有工具调用
            if (response.aiMessage().hasToolExecutionRequests()) {
                messages.add(response.aiMessage());
                // 执行工具调用
                List<ToolExecutionRequest> requests = response.aiMessage().toolExecutionRequests();
                List<ToolExecutionResultMessage> resultMessages = toolExecutionPort.executeTool(sessionId, requests, definition);
                messages.addAll(resultMessages);
                chatMemoryStore.updateMessages(sessionId, messages);
            } else {
                // 当前这轮 LLM 没有再请求工具，终止循环
                finished = true;
                break;
            }
        }
        // 超过最大思考轮数
        if (!finished) {
            return Flux.error(new RuntimeException("Agent exceeded max steps"));
        }

        // 流式生成最终回复并持久化聊天记录
        StringBuilder finalMessageBuilder = new StringBuilder();
        List<ChatMessage> finalMessages = new ArrayList<>(messages);
        return llmPort.StreamingChat(definition, finalMessages, List.of())
                .doOnNext(finalMessageBuilder::append)
                .doOnComplete(() -> {
                    // 清理工具调用协议
                    finalMessages.add(new AiMessage(finalMessageBuilder.toString()));
                    List<ChatMessage> cleanedMessages = cleanToolExecutionMessage(finalMessages);
                    chatMemoryStore.updateMessages(sessionId, cleanedMessages);
                });
    }

    /**
     * 创建新智能体会话
     *
     * @param userId    用户ID
     * @param agentName 智能体名称
     * @return 会话视图
     */
    @Override
    public ChatSessionVO createNewChatSession(Long userId, String agentName) {
        LocalDateTime now = LocalDateTime.now();

        // 缓存取映射表
        Map<String, Long> cache = cacheService.get(AGENT_NAME_ID_MAP_CACHE_KEY);
        if (cache == null) {
            Map<Long, AgentEntity> agentEntityMap = agentRepository.getEntityMap();
            cache = agentEntityMap.values().stream()
                    .collect(Collectors.toMap(AgentEntity::getName, AgentEntity::getId));
            cacheService.set(AGENT_NAME_ID_MAP_CACHE_KEY, cache, 1, TimeUnit.MINUTES);
        }

        // 映射表中无对应Agent则抛异常
        if (!cache.containsKey(agentName)) {
            throw new ApplicationException(AgentErrorCodeEnum.AGENT_NOT_FOUND);
        }
        // 生成雪花ID
        Long sessionId = HutoolSnowflakeIdGenerator.generateLongId();
        ChatSessionEntity chatSessionEntity = ChatSessionEntity.builder()
                .id(sessionId)
                .agentId(cache.get(agentName))
                .userId(userId)
                .title(null)    // 等待ai生成
                .metadata(null) // 预留扩展
                .createdAt(now)
                .updatedAt(now)
                .build();
        chatSessionRepository.save(chatSessionEntity);

        // 缓存业务上下文
        chatSessionContextPort.saveContext(sessionId, ChatSessionContext.builder().userId(userId).build());

        return ChatSessionVO.builder()
                .sessionId(chatSessionEntity.getId())
                .agentName(agentName)
                .userId(chatSessionEntity.getUserId())
                .title(chatSessionEntity.getTitle())
                .metadata(chatSessionEntity.getMetadata())
                .createdAt(chatSessionEntity.getCreatedAt())
                .updatedAt(chatSessionEntity.getUpdatedAt())
                .build();
    }

    /**
     * 获取聊天会话列表
     *
     * @param userId 用户ID
     * @return 会话视图列表
     */
    @Override
    public List<ChatSessionVO> getChatSessions(Long userId) {
        List<ChatSessionEntity> entities = chatSessionRepository.findByUserId(userId);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        Map<Long, AgentEntity> agentEntityMap = agentRepository.getEntityMap();

        return entities.stream()
                .map( entity -> ChatSessionVO.builder()
                        .sessionId(entity.getId())
                        .agentName(agentEntityMap.get(entity.getAgentId()).getName())
                        .userId(entity.getUserId())
                        .title(entity.getTitle())
                        .metadata(entity.getMetadata())
                        .createdAt(entity.getCreatedAt())
                        .updatedAt(entity.getUpdatedAt())
                        .build())
                .toList();
    }

    /**
     * 更新会话标题
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param title 新标题
     */
    @Override
    public void updateChatSessionTitle(Long userId, Long sessionId, String title) {
        ChatSessionEntity sessionEntity = getOwnedSession(userId, sessionId);
        sessionEntity.setTitle(title == null ? null : title.trim());
        sessionEntity.setUpdatedAt(LocalDateTime.now());
        chatSessionRepository.update(sessionEntity);
    }

    /**
     * 硬删除单个会话
     *
     * @param userId 用户ID
     * @param sessionId 会话ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChatSession(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        // 清理聊天消息(数据库 + 缓存)
        chatMemoryStore.deleteMessages(sessionId);
        // 清理会话业务上下文缓存
        chatSessionContextPort.deleteContext(sessionId);
        // 硬删除会话记录
        chatSessionRepository.deleteById(sessionId);
    }

    /**
     * 获取会话的聊天记录
     *
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 聊天记录视图
     */
    @Override
    public List<ChatMessageVO> getChatMessages(Long userId, Long sessionId) {
        getOwnedSession(userId, sessionId);
        List<ChatMessage> messages = chatMemoryStore.getMessages(sessionId);
        return ChatMessageConverter.toVOList(messages);
    }


    private List<ChatMessage> buildThinkMessages(List<ChatMessage> messages, AgentDefinition definition, ChatSessionContext context) {
        List<ChatMessage> thinkMessages = new ArrayList<>();
        thinkMessages.add(new SystemMessage(definition.getSystemPrompt().replace("{{ctx}}", renderContext(context))));

        // 把原有历史里除 system message 之外的消息加进去
        for (ChatMessage message : messages) {
            if (!(message instanceof SystemMessage)) {
                thinkMessages.add(message);
            }
        }
        return thinkMessages;
    }

    /**
     * 清理工具调用信息
     */
    private List<ChatMessage> cleanToolExecutionMessage(List<ChatMessage> messages) {
        List<ChatMessage> cleaned = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message instanceof ToolExecutionResultMessage) {
                continue;
            }
            if (message instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                continue;
            }
            cleaned.add(message);
        }
        return cleaned;
    }

    private String renderContext(ChatSessionContext context) {
        if (context == null) {
            return """
        - 当前用户未选中题目
        """;
        }

        boolean hasCollection = context.getCollectionIds() != null && !context.getCollectionIds().isEmpty();
        int selectedQuestionCount = context.getQuestionIds() == null ? 0 : context.getQuestionIds().size();

        String selectionHint = selectedQuestionCount > 0
                ? "当前已有用户选中的题目；如果消息记录中无题干内容，直接调用查看题目详情工具获取"
                : "当前没有选中题目；若用户说“这道题”但未提供更多描述，引导用户选中题目。";

        return """
        - 当前题集：%s
        - 已选题目数量：%d
        - %s
        - 不要向用户索要或暴露任何内部ID
        """.formatted(
                hasCollection ? "已选中" : "未选中",
                selectedQuestionCount,
                selectionHint
        );
    }

    private ChatSessionEntity getOwnedSession(Long userId, Long sessionId) {
        ChatSessionEntity sessionEntity = chatSessionRepository.findById(sessionId);
        if (sessionEntity == null) {
            throw new ApplicationException(AgentErrorCodeEnum.AGENT_CHAT_SESSION_NOT_FOUND);
        }
        if (!Objects.equals(sessionEntity.getUserId(), userId)) {
            throw new ApplicationException(CommonResultCodeEnum.NO_PERMISSION);
        }
        return sessionEntity;
    }
}
