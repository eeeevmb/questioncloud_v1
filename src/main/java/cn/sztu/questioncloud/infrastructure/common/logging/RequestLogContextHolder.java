package cn.sztu.questioncloud.infrastructure.common.logging;

public final class RequestLogContextHolder {
    private static final ThreadLocal<RequestLogContext> HOLDER = new ThreadLocal<>();

    private RequestLogContextHolder() {
    }

    public static void set(RequestLogContext context) {
        HOLDER.set(context);
    }

    public static RequestLogContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
