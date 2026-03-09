package cn.sztu.questioncloud.application.ai.helper;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.Content;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionRequest;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionResult;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

import java.time.LocalDateTime;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 将 LangChain4j {@link ChatMessage} 转换为对外展示的 {@link ChatMessageVO}。
 */
public final class ChatMessageConverter {

    private ChatMessageConverter() {
    }

    public static List<ChatMessageVO> toVOList(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.stream()
                .map(ChatMessageConverter::toVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static ChatMessageVO toVO(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return switch (message.type()) {
            case SYSTEM -> fromSystem((SystemMessage) message);
            case USER -> fromUser((UserMessage) message);
            case AI -> fromAi((AiMessage) message);
            case TOOL_EXECUTION_RESULT -> fromToolExecutionResult((ToolExecutionResultMessage) message);
            case CUSTOM -> fromCustom((CustomMessage) message);
        };
    }

    private static ChatMessageVO fromSystem(SystemMessage systemMessage) {
        return base(systemMessage)
                .text(systemMessage.text())
                .build();
    }

    private static ChatMessageVO fromUser(UserMessage userMessage) {
        List<Content> contents = toContents(userMessage.contents());
        String plainText = contents.stream()
                .map(Content::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        return base(userMessage)
                .text(plainText.isEmpty() ? null : plainText)
                .contents(contents)
                .attributes(safeMap(userMessage.attributes()))
                .build();
    }

    private static ChatMessageVO fromAi(AiMessage aiMessage) {
        return base(aiMessage)
                .text(aiMessage.text())
                .thinking(aiMessage.thinking())
                .toolExecutionRequests(toToolExecutionRequests(aiMessage.toolExecutionRequests()))
                .attributes(safeMap(aiMessage.attributes()))
                .build();
    }

    private static ChatMessageVO fromToolExecutionResult(ToolExecutionResultMessage resultMessage) {
        return base(resultMessage)
                .toolExecutionResult(ToolExecutionResult.builder()
                        .id(resultMessage.id())
                        .toolName(resultMessage.toolName())
                        .text(resultMessage.text())
                        .build())
                .text(resultMessage.text())
                .build();
    }

    private static ChatMessageVO fromCustom(CustomMessage customMessage) {
        return base(customMessage)
                .attributes(safeMap(customMessage.attributes()))
                .build();
    }

    public static ChatMessageEntity toEntity(ChatMessage message, Long sessionId) {
        if (message == null) {
            return null;
        }
        ChatMessageEntity entity = baseEntity(message, sessionId);
        return switch (message.type()) {
            case SYSTEM -> fromSystemEntity(entity, (SystemMessage) message);
            case USER -> fromUserEntity(entity, (UserMessage) message);
            case AI -> fromAiEntity(entity, (AiMessage) message);
            case TOOL_EXECUTION_RESULT -> fromToolExecutionResultEntity(entity, (ToolExecutionResultMessage) message);
            case CUSTOM -> fromCustomEntity(entity, (CustomMessage) message);
        };
    }

    private static ChatMessageVO.ChatMessageVOBuilder base(ChatMessage message) {
        return ChatMessageVO.builder()
                .type(message.type().name())
                .role(resolveRole(message));
    }

    private static ChatMessageEntity baseEntity(ChatMessage message, Long sessionId) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSessionId(sessionId);
        entity.setType(message.type().name());
        entity.setRole(resolveRole(message));
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static ChatMessageEntity fromSystemEntity(ChatMessageEntity entity, SystemMessage systemMessage) {
        entity.setTextContent(systemMessage.text());
        return entity;
    }

    private static ChatMessageEntity fromUserEntity(ChatMessageEntity entity, UserMessage userMessage) {
        List<Content> contents = toContents(userMessage.contents());
        String plainText = contents.stream()
                .map(Content::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));
        entity.setTextContent(plainText.isEmpty() ? null : plainText);
        entity.setContent(contents);
        entity.setAttributes(safeMap(userMessage.attributes()));
        return entity;
    }

    private static ChatMessageEntity fromAiEntity(ChatMessageEntity entity, AiMessage aiMessage) {
        entity.setTextContent(aiMessage.text());
        entity.setThinkingContent(aiMessage.thinking());
        entity.setToolExecutionRequest(toToolExecutionRequests(aiMessage.toolExecutionRequests()));
        entity.setAttributes(safeMap(aiMessage.attributes()));
        return entity;
    }

    private static ChatMessageEntity fromToolExecutionResultEntity(ChatMessageEntity entity,
                                                                  ToolExecutionResultMessage resultMessage) {
        entity.setToolExecutionResult(ToolExecutionResult.builder()
                .id(resultMessage.id())
                .toolName(resultMessage.toolName())
                .text(resultMessage.text())
                .build());
        entity.setTextContent(resultMessage.text());
        return entity;
    }

    private static ChatMessageEntity fromCustomEntity(ChatMessageEntity entity, CustomMessage customMessage) {
        entity.setAttributes(safeMap(customMessage.attributes()));
        return entity;
    }

    private static List<Content> toContents(List<dev.langchain4j.data.message.Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        return contents.stream()
                .map(ChatMessageConverter::toContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Content toContent(dev.langchain4j.data.message.Content messageContent) {
        if (messageContent == null) {
            return null;
        }
        Content.ContentBuilder builder = Content.builder()
                .type(messageContent.type().name());

        if (messageContent instanceof TextContent textContent) {
            builder.text(textContent.text());
        } else if (messageContent instanceof ImageContent imageContent) {
            builder.detailLevel(Optional.ofNullable(imageContent.detailLevel()).map(Enum::name).orElse(null));
            Optional.ofNullable(imageContent.image()).ifPresent(image -> fillMedia(builder, image.url(), image.mimeType(), image.toString()));
        } else if (messageContent instanceof AudioContent audioContent) {
            Optional.ofNullable(audioContent.audio()).ifPresent(audio -> fillMedia(builder, audio.url(), audio.mimeType(), audio.toString()));
        } else if (messageContent instanceof VideoContent videoContent) {
            Optional.ofNullable(videoContent.video()).ifPresent(video -> fillMedia(builder, video.url(), video.mimeType(), video.toString()));
        } else if (messageContent instanceof PdfFileContent pdfFileContent) {
            Optional.ofNullable(pdfFileContent.pdfFile()).ifPresent(pdf -> fillMedia(builder, pdf.url(), pdf.mimeType(), pdf.toString()));
        } else {
            builder.text(String.valueOf(messageContent));
        }
        return builder.build();
    }

    private static void fillMedia(Content.ContentBuilder builder,
                                  URI uri,
                                  String mimeType,
                                  String fallbackText) {
        builder.url(uri != null ? uri.toString() : null);
        builder.mimeType(mimeType);
        builder.text(fallbackText);
    }

    private static List<ToolExecutionRequest> toToolExecutionRequests(List<dev.langchain4j.agent.tool.ToolExecutionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(request -> ToolExecutionRequest.builder()
                        .id(request.id())
                        .name(request.name())
                        .arguments(safeMap(request.arguments()))
                        .build())
                .collect(Collectors.toList());
    }

    private static Map<String, Object> safeMap(Object source) {
        if (!(source instanceof Map<?, ?> raw) || raw.isEmpty()) {
            return Collections.emptyMap();
        }
        return raw.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> String.valueOf(entry.getKey()),
                        Map.Entry::getValue,
                        (left, right) -> right));
    }

    private static String resolveRole(ChatMessage message) {
        return switch (message.type()) {
            case SYSTEM -> "system";
            case USER -> "user";
            case AI -> "assistant";
            case TOOL_EXECUTION_RESULT -> "tool";
            case CUSTOM -> "custom";
        };
    }
}
