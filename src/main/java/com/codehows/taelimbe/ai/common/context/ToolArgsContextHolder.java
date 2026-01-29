package com.codehows.taelimbe.ai.common.context;

import java.util.concurrent.ConcurrentHashMap;

public class ToolArgsContextHolder {
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> STORE = new ConcurrentHashMap<>();
    private static final ThreadLocal<String> CURRENT_ID = new ThreadLocal<>();

    // 현재 스레드를 contextId에 바인딩
    public static void bind(String contextId) {
        CURRENT_ID.set(contextId);
        STORE.computeIfAbsent(contextId, k -> new ConcurrentHashMap<>());
    }

    public static void setToolArgs(String key, String value) {
        String id = CURRENT_ID.get();
        if (id != null) {
            ConcurrentHashMap<String, String> map = STORE.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
            if (value != null) {
                map.put(key, value);
            } else {
                map.remove(key);
            }
        }
    }

    public static String getToolArgs(String key) {
        String id = CURRENT_ID.get();
        if (id == null) return null;
        var map = STORE.get(id);
        return map != null ? map.get(key) : null;
    }

    // contextId 지정해서 정리 (콜백 스레드에서 사용)
    public static void clear(String contextId) {
        STORE.remove(contextId);
        if (contextId.equals(CURRENT_ID.get())) {
            CURRENT_ID.remove();
        }
    }

    public static void clear() {
        String id = CURRENT_ID.get();
        if (id != null) {
            STORE.remove(id);
            CURRENT_ID.remove();
        }
    }
}
