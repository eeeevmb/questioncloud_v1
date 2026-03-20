package cn.sztu.questioncloud.common.util;

import reactor.core.publisher.Mono;

public final class ReactorSessionContext {
    public static final String KEY = "sessionId";

    public static Mono<String> getSessionId() {
        return Mono.deferContextual(ctx -> Mono.justOrEmpty(ctx.getOrEmpty(KEY))
                .map(Object::toString));
    }
}
