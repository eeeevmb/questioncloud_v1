package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionVO {
    /**
     * 会话记忆ID(由HuTool生成的雪花ID)
     */
    private String memoryId;

    /**
     * 用户ID
     */
    private Long userId;
}
