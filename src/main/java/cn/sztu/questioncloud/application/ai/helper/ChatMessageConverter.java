package cn.sztu.questioncloud.application.ai.helper;

import cn.sztu.questioncloud.web.rest.v1.ai.vo.ChatMessageVO;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.AudioContent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.CustomMessage;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.PdfFileContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.VideoContent;

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
        return base(systemMessage, "system")
                .text(systemMessage.text())
                .build();
    }

    private static ChatMessageVO fromUser(UserMessage userMessage) {
        List<ChatMessageVO.ContentVO> content = toContentVOs(userMessage.contents());
        String plainText = content.stream()
                .map(ChatMessageVO.ContentVO::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"));

        return base(userMessage, "user")
                .text(plainText.isEmpty() ? null : plainText)
                .contents(content)
                .attributes(safeMap(userMessage.attributes()))
                .build();
    }

    private static ChatMessageVO fromAi(AiMessage aiMessage) {
        return base(aiMessage, "assistant")
                .text(aiMessage.text())
                .thinking(aiMessage.thinking())
                .toolExecutionRequests(toToolExecutionRequestVOs(aiMessage.toolExecutionRequests()))
                .attributes(safeMap(aiMessage.attributes()))
                .build();
    }

    private static ChatMessageVO fromToolExecutionResult(ToolExecutionResultMessage resultMessage) {
        return base(resultMessage, "tool")
                .toolExecutionResult(ChatMessageVO.ToolExecutionResultVO.builder()
                        .id(resultMessage.id())
                        .toolName(resultMessage.toolName())
                        .text(resultMessage.text())
                        .build())
                .text(resultMessage.text())
                .build();
    }

    private static ChatMessageVO fromCustom(CustomMessage customMessage) {
        return base(customMessage, "custom")
                .attributes(safeMap(customMessage.attributes()))
                .build();
    }

    private static ChatMessageVO.ChatMessageVOBuilder base(ChatMessage message, String role) {
        return ChatMessageVO.builder()
                .type(message.type().name())
                .role(role);
    }

    private static List<ChatMessageVO.ContentVO> toContentVOs(List<Content> contents) {
        if (contents == null || contents.isEmpty()) {
            return Collections.emptyList();
        }
        return contents.stream()
                .map(ChatMessageConverter::toContentVO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static ChatMessageVO.ContentVO toContentVO(Content content) {
        if (content == null) {
            return null;
        }
        ChatMessageVO.ContentVO.ContentVOBuilder builder = ChatMessageVO.ContentVO.builder()
                .type(content.type().name());

        if (content instanceof TextContent textContent) {
            builder.text(textContent.text());
        } else if (content instanceof ImageContent imageContent) {
            builder.detailLevel(Optional.ofNullable(imageContent.detailLevel()).map(Enum::name).orElse(null));
            Optional.ofNullable(imageContent.image()).ifPresent(image -> fillMedia(builder, image.url(), image.mimeType(), image.toString()));
        } else if (content instanceof AudioContent audioContent) {
            Optional.ofNullable(audioContent.audio()).ifPresent(audio -> fillMedia(builder, audio.url(), audio.mimeType(), audio.toString()));
        } else if (content instanceof VideoContent videoContent) {
            Optional.ofNullable(videoContent.video()).ifPresent(video -> fillMedia(builder, video.url(), video.mimeType(), video.toString()));
        } else if (content instanceof PdfFileContent pdfFileContent) {
            Optional.ofNullable(pdfFileContent.pdfFile()).ifPresent(pdf -> fillMedia(builder, pdf.url(), pdf.mimeType(), pdf.toString()));
        } else {
            builder.text(String.valueOf(content));
        }
        return builder.build();
    }

    private static void fillMedia(ChatMessageVO.ContentVO.ContentVOBuilder builder,
                                  URI uri,
                                  String mimeType,
                                  String fallbackText) {
        builder.url(uri != null ? uri.toString() : null);
        builder.mimeType(mimeType);
        builder.text(fallbackText);
    }

    private static List<ChatMessageVO.ToolExecutionRequestVO> toToolExecutionRequestVOs(List<ToolExecutionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(request -> ChatMessageVO.ToolExecutionRequestVO.builder()
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
}
