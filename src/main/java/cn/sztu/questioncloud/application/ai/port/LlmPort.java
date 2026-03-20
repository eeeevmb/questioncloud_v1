package cn.sztu.questioncloud.application.ai.port;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.importer.dto.QuestionDraft;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 大语言模型端口，提供外部模型能力
 */
public interface LlmPort {
    /**
     * 同步返回聊天响应（用于决定是否调用工具）
     *
     * @param definition         智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 聊天响应
     */
    ChatResponse chat(AgentDefinition definition,
                       List<ChatMessage> messages,
                       List<ToolSpecification> toolSpecifications);

    /**
     * 生成会话标题
     * @param userMessage 用户消息
     * @return 会话标题
     */
    String generateSessionTitle(UserMessage userMessage);

    /**
     * 根据用户描述生成题目草稿
     * @param userInput 用户输入
     * @return 题目草稿
     */
    QuestionDraft generateQuestionDraft(String userInput);

    // 流式接口

    /**
     * 流式返回聊天响应（用于生成最终回复）
     * @param agent              智能体定义
     * @param messages           聊天记录
     * @param toolSpecifications 工具介绍
     * @return 流式聊天响应
     */
    Flux<String> StreamingChat(AgentDefinition agent,
                      List<ChatMessage> messages,
                      List<ToolSpecification> toolSpecifications);
}
