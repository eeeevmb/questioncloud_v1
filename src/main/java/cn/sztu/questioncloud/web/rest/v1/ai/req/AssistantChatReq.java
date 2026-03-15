package cn.sztu.questioncloud.web.rest.v1.ai.req;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatReq {
    /**
     * 会话ID
     */
    private Long sessionId;

    /**
     * 用户消息
     */
    private String message;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 上下文
     */
    private Context context;

    @Data
    public static class Context {
        Long collectionId;
        List<Long> selectedQuestionIds;
    }
}
