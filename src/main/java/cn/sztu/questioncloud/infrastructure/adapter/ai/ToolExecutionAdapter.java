package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.AgentDefinition;
import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.port.ToolExecutionPort;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.CreateQuestionArgs;
import cn.sztu.questioncloud.infrastructure.common.ai.dto.SearchQuestionArgs;
import cn.sztu.questioncloud.infrastructure.common.ai.tool.QuestionDomainTool;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import com.fasterxml.jackson.core.io.JsonEOFException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayDeque;
import java.util.Deque;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具执行实现，目前的实现思路比较简陋，用switch路由对应工具
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolExecutionAdapter implements ToolExecutionPort {
    private final CacheService cacheService;
    private final ObjectMapper objectMapper;
    private final QuestionDomainTool questionDomainTool;

    @Override
    public List<ToolExecutionResultMessage> executeTool(Long sessionId, List<ToolExecutionRequest> requests, AgentDefinition definition) {
        // 从redis获取业务上下文
        ChatSessionContext context = cacheService.get(CacheKeyUtil.chatContextKey(String.valueOf(sessionId)));
        List<ToolExecutionResultMessage> results = new ArrayList<>();
        if (context == null) {
            // 有多少个请求就构建多少个返回结果
            for (ToolExecutionRequest request : requests) {
                results.add(ToolExecutionResultMessage.from(request, "工具执行失败: IllegalStateException: 会话上下文不存在"));
            }
            return results;
        }

        // 遍历工具请求列表
        for (ToolExecutionRequest request : requests) {
            try {
                String executionResult = switch (request.name()) {
                    case "createQuestion" -> createQuestion(context, request.arguments());
                    case "searchQuestion" -> searchQuestion(context, request.arguments());
                    case "getQuestionDetail" -> getQuestionDetail(context);
                    // 工具扩展
                    default -> "不支持的工具";
                };
                results.add(ToolExecutionResultMessage.from(request, executionResult));
            } catch (Exception e) {
                log.error("工具执行失败, toolName={}, args={}", request.name(), request.arguments(), e);
                results.add(ToolExecutionResultMessage.from(
                        request,
                        "工具执行失败: " + e.getClass().getSimpleName() + ": " + (e.getMessage() == null ? "unknown" : e.getMessage())
                ));
            }
        }

        return results;

    }


    // 工具方法
    /**
     * 创建题目
     * @param context  业务上下文
     * @param argsJson 序列化参数
     * @return 工具执行结果
     */
    private String createQuestion(ChatSessionContext context, String argsJson) throws Exception {
        // 兼容两种入参格式：
        // 1) { "typeCode": "...", ... }
        // 2) { "args": { "typeCode": "...", ... } }
        JsonNode payload = unwrapPayload(argsJson);
        CreateQuestionArgs createQuestionArgs = objectMapper.treeToValue(payload, CreateQuestionArgs.class);
        // 序列化执行结果
        return objectMapper.writeValueAsString(questionDomainTool.createQuestion(context, createQuestionArgs));
    }
    /**
     * 获取题目详细信息
     * @param context  业务上下文
     * @return 工具执行结果
     */
    private String getQuestionDetail(ChatSessionContext context) throws Exception {
        return objectMapper.writeValueAsString(questionDomainTool.getQuestionDetail(context));
    }

    /**
     *
     * @param context  业务上下文
     * @param argsJson 序列化参数
     * @return 工具执行结果
     */
    private String searchQuestion(ChatSessionContext context, String argsJson) throws Exception {
        // 兼容两种入参格式：
        // 1) { "query":"...", "ragSearchParam":{...} }
        // 2) { "args": { "query":"...", "ragSearchParam":{...} } }
        JsonNode payload = unwrapPayload(argsJson);
        SearchQuestionArgs searchQuestionArgs = objectMapper.treeToValue(payload, SearchQuestionArgs.class);
        // 序列化执行结果
        return objectMapper.writeValueAsString(questionDomainTool.searchQuestion(context, searchQuestionArgs));
    }

    private JsonNode unwrapPayload(String rawArgs) throws JsonProcessingException {
        if (rawArgs == null || rawArgs.isBlank()) {
            throw new IllegalArgumentException("工具参数为空");
        }

        String normalized = normalizeToolArgs(rawArgs);
        JsonNode root;
        try {
            root = objectMapper.readTree(normalized);
        } catch (JsonEOFException eof) {
            String repaired = repairTruncatedJson(normalized);
            root = objectMapper.readTree(repaired);
        }

        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("工具参数为空JSON");
        }

        if (root.hasNonNull("args") && root.get("args").isObject()) {
            return root.get("args");
        }
        return root;
    }

    private String normalizeToolArgs(String rawArgs) {
        String normalized = rawArgs.trim();

        if (normalized.startsWith("```json")) {
            normalized = normalized.substring(7).trim();
        } else if (normalized.startsWith("```")) {
            normalized = normalized.substring(3).trim();
        }
        if (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }

        return normalized;
    }

    private String repairTruncatedJson(String json) {
        StringBuilder sb = new StringBuilder(json);
        Deque<Character> stack = new ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '{' || c == '[') {
                stack.push(c);
            } else if (c == '}' && !stack.isEmpty() && stack.peek() == '{') {
                stack.pop();
            } else if (c == ']' && !stack.isEmpty() && stack.peek() == '[') {
                stack.pop();
            }
        }

        if (inString) {
            sb.append('"');
        }

        while (!stack.isEmpty()) {
            char open = stack.pop();
            sb.append(open == '{' ? '}' : ']');
        }
        return sb.toString();
    }
}
