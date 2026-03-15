package cn.sztu.questioncloud.infrastructure.adapter.ai;

import cn.sztu.questioncloud.application.ai.dto.ChatSessionContext;
import cn.sztu.questioncloud.application.ai.port.ChatSessionContextPort;
import cn.sztu.questioncloud.common.util.CacheKeyUtil;
import cn.sztu.questioncloud.infrastructure.common.cache.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChatSessionContextAdapter implements ChatSessionContextPort {
    private final CacheService cacheService;

    private static final Long CACHE_TTL_MINUTES = 30L;
    /**
     * 在缓存中写入业务上下文
     *
     * @param sessionId          会话ID
     * @param chatSessionContext 上下文
     */
    @Override
    public void saveContext(Long sessionId, ChatSessionContext chatSessionContext) {
        cacheService.set(
                CacheKeyUtil.chatContextKey(String.valueOf(sessionId)),
                chatSessionContext,
                CACHE_TTL_MINUTES,
                TimeUnit.MINUTES);
    }
}
