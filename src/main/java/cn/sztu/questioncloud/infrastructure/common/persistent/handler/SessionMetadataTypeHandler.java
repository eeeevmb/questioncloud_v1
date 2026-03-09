package cn.sztu.questioncloud.infrastructure.common.persistent.handler;

import cn.sztu.questioncloud.infrastructure.common.persistent.entity.agent.dto.SessionMetadata;

/**
 * chat_session.metadata JSON 列转 SessionMetadata。
 */
public class SessionMetadataTypeHandler extends JacksonTypeHandler<SessionMetadata> {

    public SessionMetadataTypeHandler() {
        super(SessionMetadata.class);
    }
}
