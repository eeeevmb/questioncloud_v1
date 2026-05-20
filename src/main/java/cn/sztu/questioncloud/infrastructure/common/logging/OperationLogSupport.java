package cn.sztu.questioncloud.infrastructure.common.logging;

import java.util.HashMap;
import java.util.Map;

public final class OperationLogSupport {
    private static final ThreadLocal<Map<String, Object>> VARIABLES = ThreadLocal.withInitial(HashMap::new);

    private OperationLogSupport() {
    }

    public static void put(String key, Object value) {
        if (key != null && value != null) {
            VARIABLES.get().put(key, value);
        }
    }

    public static Map<String, Object> snapshot() {
        return Map.copyOf(VARIABLES.get());
    }

    public static void clear() {
        VARIABLES.remove();
    }
}
