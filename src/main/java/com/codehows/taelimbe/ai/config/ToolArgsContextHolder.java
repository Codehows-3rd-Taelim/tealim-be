package com.codehows.taelimbe.ai.config;

import java.util.HashMap;
import java.util.Map;

public class ToolArgsContextHolder {
    private static final ThreadLocal<Map<String, String>> toolArgs = new ThreadLocal<>();

    public static void setToolArgs(String key, String value) {
        if(toolArgs.get() == null) {
            toolArgs.set(new HashMap<>());
        }
        toolArgs.get().put(key, value);
    }

    public static String getToolArgs(String key) {
        return toolArgs.get().get(key);
    }

    public static void clear() {
        toolArgs.remove();
    }

}
