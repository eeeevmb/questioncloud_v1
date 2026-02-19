package cn.sztu.questioncloud.web.rest.v1.ai.req;

import lombok.Data;

import java.util.List;

@Data
public class AssistantChatReq {
    /**
     * 会话记忆ID(由HuTool生成的雪花ID)
     */
    private String memoryId;

    /**
     * 消息ID
     */
    private String message;

    /**
     * 上下文
     */
    private Context context;

    @Data
    public static class Context {
        List<Long> selectedQuestionIds;
    }
}
