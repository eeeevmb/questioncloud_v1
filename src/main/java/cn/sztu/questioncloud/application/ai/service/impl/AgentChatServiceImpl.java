package cn.sztu.questioncloud.application.ai.service.impl;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.enums.AgentErrorCodeEnum;
import cn.sztu.questioncloud.application.ai.port.*;
import cn.sztu.questioncloud.application.ai.service.AgentChatService;
import cn.sztu.questioncloud.common.exception.ApplicationException;
import cn.sztu.questioncloud.infrastructure.common.ai.memory.RedisChatMemoryStore;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import cn.sztu.questioncloud.infrastructure.common.id.HutoolSnowflakeIdGenerator;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.AgentEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatSessionEntity;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatSessionVO;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.tool.ToolExecutionResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    @Override
    public Flux<String> chat(ChatSessionContext context, Long sessionId, String agentName, String userInput) {
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
            messages = new ArrayList<>();
            // TODO 生成会话标题
        }
        // 更新上下文
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
            cacheService.set(AGENT_NAME_ID_MAP_CACHE_KEY, cache);
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
        当前会话业务上下文：
        - 当前未提供可用上下文
        """;
        }

        boolean hasCollection = context.getCollectionIds() != null && !context.getCollectionIds().isEmpty();
        int selectedQuestionCount = context.getQuestionIds() == null ? 0 : context.getQuestionIds().size();

        String selectionHint = selectedQuestionCount > 0
                ? "当前已有用户选中的题目；如果用户说“这道题”“当前选中题目”“讲解一下这题”，应直接基于当前选中题目处理，不要再次追问用户选择哪一道。"
                : "当前没有已选中的题目；若用户说“这道题”但未提供更多描述，才需要先定位题目。";

        return """
        当前会话业务上下文：
        - 当前题集：%s
        - 已选题目数量：%d
        - %s
        - 不要向用户索要或暴露任何内部ID
        """.formatted(
                hasCollection ? "已绑定" : "未绑定",
                selectedQuestionCount,
                selectionHint
        );
    }
}
