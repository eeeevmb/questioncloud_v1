package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.port.LlmPort;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM适配器，提供大模型对话能力
 */
@Component
@RequiredArgsConstructor
public class LangChain4jLlmAdapter implements LlmPort {
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final ObjectMapper objectMapper;

    private static final BigDecimal DEFAULT_DIFFICULTY = BigDecimal.valueOf(0.50);
    /**
     * 同步返回聊天响应（用于决定是否调用工具）
     *
     * @param definition         智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 聊天响应
     */
    @Override
    public ChatResponse chat(AgentDefinition definition, List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .modelName(definition.getChatOptions().getModelName())
                        .temperature(definition.getChatOptions().getTemperature())
                        .maxOutputTokens(definition.getChatOptions().getMaxTokens())
                        .frequencyPenalty(definition.getChatOptions().getFrequencyPenalty())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();
        return chatModel.chat(request);
    }

    /**
     * 生成会话标题
     * @param userMessage 用户消息
     * @return 会话标题
     */
    @Override
    public String generateSessionTitle(UserMessage userMessage) {
        try {
            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                你是会话标题生成助手。
                                请根据用户首条消息生成一个简洁的中文会话标题。
                                要求：
                                1. 只输出标题本身
                                2. 不要加引号、句号、冒号、序号、解释
                                3. 不要换行
                                4. 长度控制在 8~15 个汉字
                                5. 若内容偏技术，标题尽量概括主题，不要复述整句
                                """),
                            userMessage
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.2)
                            .maxOutputTokens(30)
                            .build())
                    .build();

            String title = chatModel.chat(request).aiMessage().text();
            // 错误生成兜底
            if (title == null || title.isBlank()) {
                return "新会话";
            }

            title = title.trim()
                    .replace("\n", "")
                    .replace("\"", "")
                    .replace("“", "")
                    .replace("”", "");

            if (title.length() > 20) {
                title = title.substring(0, 20);
            }

            return title.isBlank() ? "新会话" : title;
        } catch (Exception e) {
            return "新会话";
        }
    }

    /**
     * 根据用户描述生成题目草稿
     *
     * @param userInput 用户输入
     * @return 题目草稿
     */
    @Override
    public QuestionDraft generateQuestionDraft(String userInput) {
        try {
            ResponseFormat responseFormat = ResponseFormat.builder()
                    .type(ResponseFormatType.JSON)
                    .build();

            ChatRequest request = ChatRequest.builder()
                    .messages(List.of(
                            SystemMessage.from("""
                                    你是题目草稿生成助手。
                                    你必须严格输出 QuestionDraft 的 JSON 对象，不要输出 markdown 代码块，不要输出解释。
                                    
                                    字段规则（与创建题目参数一致）：
                                    - typeCode: 必填，枚举为 single-choice/multiple-choice/true-false/fill-in/short-answer
                                    - stem: 必填
                                    - title: 必选
                                    - difficulty: 可选；若用户未指定请输出 0.50
                                    - solution: 必选
                                    - answer: fill-in/short-answer 建议提供；true-false 也可提供
                                    
                                    题型专用字段：
                                    - single-choice/multiple-choice:
                                      必须提供 options（每项含 key 和 content）与 correctOptions
                                    - true-false:
                                      必须提供 judgeAnswer，且只能是 T 或 F
                                    - fill-in/short-answer:
                                      使用 answer，且不要提供 options/correctOptions/judgeAnswer
                                    
                                    输出必须可被 JSON 反序列化为 QuestionDraft。
                                    """),
                            UserMessage.from(userInput)
                    ))
                    .parameters(ChatRequestParameters.builder()
                            .temperature(0.1)
                            .maxOutputTokens(1024)
                            .responseFormat(responseFormat)
                            .build())
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            if (aiMessage == null || aiMessage.text() == null || aiMessage.text().isBlank()) {
                throw new IllegalStateException("模型未返回可解析的题目草稿");
            }

            QuestionDraft draft = objectMapper.readValue(aiMessage.text(), QuestionDraft.class);
            if (draft.getDifficulty() == null) {
                draft.setDifficulty(DEFAULT_DIFFICULTY);
            }
            return draft;
        } catch (Exception e) {
            throw new RuntimeException("生成题目草稿失败", e);
        }
    }

    /**
     * 流式返回聊天响应（用于生成最终回复）
     *
     * @param definition         智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 流式聊天响应
     */
    @Override
    public Flux<String> StreamingChat(AgentDefinition definition, List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .parameters(ChatRequestParameters.builder()
                        .modelName(definition.getChatOptions().getModelName())
                        .temperature(definition.getChatOptions().getTemperature())
                        .maxOutputTokens(definition.getChatOptions().getMaxTokens())
                        .frequencyPenalty(definition.getChatOptions().getFrequencyPenalty())
                        .toolSpecifications(toolSpecifications)
                        .build())
                .build();

        return Flux.create(emitter -> streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                if (partialResponse != null && !partialResponse.isEmpty() && !emitter.isCancelled()) {
                    emitter.next(partialResponse);
                }
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (!emitter.isCancelled()) {
                    emitter.complete();
                }
            }

            @Override
            public void onError(Throwable error) {
                if (!emitter.isCancelled()) {
                    emitter.error(error);
                }
            }
        }), FluxSink.OverflowStrategy.BUFFER);
    }
}
