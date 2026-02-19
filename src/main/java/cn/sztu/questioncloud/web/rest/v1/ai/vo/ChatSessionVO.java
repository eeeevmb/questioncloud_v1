package cn.sztu.questioncloud.web.rest.v1.ai.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatSessionVO {
    /**
     * 会话记忆ID(UUID)
     */
    private String memoryId;

    /**
     * 题集ID
     */
    private Long collectionId;
}
