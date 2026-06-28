package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.ChatOptions;

/**
 * agent.chat_options JSON 列与 ChatOptions 之间的转换。
 */
public class ChatOptionsTypeHandler extends JacksonTypeHandler<ChatOptions> {

    public ChatOptionsTypeHandler() {
        super(ChatOptions.class);
    }
}
