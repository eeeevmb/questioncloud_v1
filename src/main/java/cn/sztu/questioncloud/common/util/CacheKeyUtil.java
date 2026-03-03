package cn.sztu.questioncloud.common.util;

/**
 * 缓存键获取工具
 */
public class CacheKeyUtil {
    public static final String QUESTION_DETAIL_PREFIX = "question:detail:";

    private static final String AGENT_CHAT_MEMORY_PREFIX = "chat:";
    private static final String AGENT_CHAT_CONTEXT_PREFIX = "chat:ctx:";

    /**
     * 获取题目详情缓存key
     * @param questionId 题目ID
     * @return 缓存key
     */
    public static String questionDetailKey(String questionId) {
        return QUESTION_DETAIL_PREFIX + questionId;
    }

    /**
     * 获取会话记忆缓存key
     * @param memoryId 会话记忆ID
     * @return 缓存key
     */
    public static String chatMemoryKey(String memoryId) {
        return AGENT_CHAT_MEMORY_PREFIX + memoryId;
    }

    /**
     * 获取上下文缓存key
     * @param memoryId 会话记忆ID
     * @return 缓存key
     */
    public static String chatContextKey(String memoryId) {
        return AGENT_CHAT_CONTEXT_PREFIX + memoryId;
    }
}
