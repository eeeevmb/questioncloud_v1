package cn.sztu.questioncloud.web.rest.v1.ai.req;

import lombok.Data;

@Data
public class ChatReq {
    /**
     * 会话记忆ID(由HuTool生成的雪花ID)
     */
    private String memoryId;

    /**
     * 消息ID
     */
    private String message;
}
