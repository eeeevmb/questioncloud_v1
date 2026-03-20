package cn.sztu.questioncloud.application.ai.helper;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.ChatMessageEntity;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.Content;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionRequest;
import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ToolExecutionResult;
import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

import cn.hutool.core.util.IdUtil;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

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

    public static List<ChatMessage> toMessages(List<ChatMessageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(ChatMessageConverter::toMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static ChatMessage toMessage(ChatMessageEntity entity) {
        if (entity == null || entity.getType() == null) {
            return null;
        }
        final ChatMessageType type;
        try {
            type = ChatMessageType.valueOf(entity.getType());
        } catch (IllegalArgumentException ex) {
            return null;
        }
        return switch (type) {
            case SYSTEM -> toSystemMessage(entity);
            case USER -> toUserMessage(entity);
            case AI -> toAiMessage(entity);
            case TOOL_EXECUTION_RESULT -> toToolExecutionResultMessage(entity);
            case CUSTOM -> toCustomMessage(entity);
        };
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
        entity.setId(IdUtil.getSnowflakeNextId());
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

    private static ChatMessage toSystemMessage(ChatMessageEntity entity) {
        return SystemMessage.from(entity.getTextContent());
    }

    private static ChatMessage toUserMessage(ChatMessageEntity entity) {
        List<dev.langchain4j.data.message.Content> contents = toMessageContents(entity.getContent());
        if (contents.isEmpty() && entity.getTextContent() != null) {
            contents = List.of(TextContent.from(entity.getTextContent()));
        }
        return UserMessage.builder()
                .contents(contents)
                .attributes(emptyIfNull(entity.getAttributes()))
                .build();
    }

    private static ChatMessage toAiMessage(ChatMessageEntity entity) {
        return AiMessage.builder()
                .text(entity.getTextContent())
                .thinking(entity.getThinkingContent())
                .toolExecutionRequests(toLangChainToolRequests(entity.getToolExecutionRequest()))
                .attributes(emptyIfNull(entity.getAttributes()))
                .build();
    }

    private static ChatMessage toToolExecutionResultMessage(ChatMessageEntity entity) {
        ToolExecutionResult result = entity.getToolExecutionResult();
        if (result == null) {
            return ToolExecutionResultMessage.from(null, null, entity.getTextContent());
        }
        return ToolExecutionResultMessage.from(result.getId(), result.getToolName(), result.getText());
    }

    private static ChatMessage toCustomMessage(ChatMessageEntity entity) {
        return CustomMessage.from(emptyIfNull(entity.getAttributes()));
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

    private static List<dev.langchain4j.data.message.Content> toMessageContents(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return new ArrayList<>();
        }
        return contents.stream()
                .map(ChatMessageConverter::toMessageContent)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static dev.langchain4j.data.message.Content toMessageContent(Content content) {
        if (content == null || content.getType() == null) {
            return null;
        }
        final ContentType type;
        try {
            type = ContentType.valueOf(content.getType());
        } catch (IllegalArgumentException ex) {
            return toFallbackTextContent(content.getText());
        }
        return switch (type) {
            case TEXT -> toFallbackTextContent(content.getText());
            case IMAGE -> toImageContent(content);
            case AUDIO -> toAudioContent(content);
            case VIDEO -> toVideoContent(content);
//            case PDF_FILE -> toPdfFileContent(content);
            default -> toFallbackTextContent(content.getText());
        };
    }

    private static dev.langchain4j.data.message.Content toFallbackTextContent(String text) {
        return text == null ? null : TextContent.from(text);
    }

    private static dev.langchain4j.data.message.Content toImageContent(Content content) {
        if (content.getUrl() == null) {
            return null;
        }
        ImageContent.DetailLevel detailLevel = parseDetailLevel(content.getDetailLevel());
        if (content.getMimeType() != null && detailLevel != null) {
            return ImageContent.from(content.getUrl(), content.getMimeType(), detailLevel);
        }
        if (content.getMimeType() != null) {
            return ImageContent.from(content.getUrl(), content.getMimeType());
        }
        if (detailLevel != null) {
            return ImageContent.from(content.getUrl(), detailLevel);
        }
        return ImageContent.from(content.getUrl());
    }

    private static dev.langchain4j.data.message.Content toAudioContent(Content content) {
        if (content.getUrl() == null) {
            return null;
        }
        if (content.getMimeType() != null) {
            return AudioContent.from(content.getUrl(), content.getMimeType());
        }
        return AudioContent.from(content.getUrl());
    }

    private static dev.langchain4j.data.message.Content toVideoContent(Content content) {
        if (content.getUrl() == null) {
            return null;
        }
        if (content.getMimeType() != null) {
            return VideoContent.from(content.getUrl(), content.getMimeType());
        }
        return VideoContent.from(content.getUrl());
    }

    private static dev.langchain4j.data.message.Content toPdfFileContent(Content content) {
        if (content.getUrl() == null) {
            return null;
        }
        if (content.getMimeType() != null) {
            return PdfFileContent.from(content.getUrl(), content.getMimeType());
        }
        return PdfFileContent.from(content.getUrl());
    }

    private static ImageContent.DetailLevel parseDetailLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ImageContent.DetailLevel.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
                        .arguments(parseArguments(request.arguments()))
                        .build())
                .collect(Collectors.toList());
    }

    private static List<dev.langchain4j.agent.tool.ToolExecutionRequest> toLangChainToolRequests(List<ToolExecutionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(ChatMessageConverter::toLangChainToolExecutionRequest)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static dev.langchain4j.agent.tool.ToolExecutionRequest toLangChainToolExecutionRequest(ToolExecutionRequest request) {
        if (request == null) {
            return null;
        }
        return dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                .id(request.getId())
                .name(request.getName())
                .arguments(toArgumentsJson(request.getArguments()))
                .build();
    }

    private static String toArgumentsJson(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize tool arguments", e);
        }
    }

    private static Map<String, Object> parseArguments(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(json, MAP_TYPE);
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    private static Map<String, Object> emptyIfNull(Map<String, Object> source) {
        return source == null || source.isEmpty() ? Collections.emptyMap() : source;
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
