package com.codehows.taelimbe.ai.config;

import java.util.HashMap;
import java.util.Map;

public class ToolArgsContextHolder {
    private static final ThreadLocal<Map<String, String>> toolArgs = new ThreadLocal<>();

//    public static void setToolArgs(String key, String value) {
//        if(toolArgs.get() == null) {
//            toolArgs.set(new HashMap<>());
//        }
//        toolArgs.get().put(key, value);
//    }

    //    public static String getToolArgs(String key) {
//        return toolArgs.get().get(key);
//    }
    public static void setToolArgs(String key, String value) {
        Map<String, String> map = toolArgs.get();
        if (map == null) {
            map = new HashMap<>();
            toolArgs.set(map);
        }
        map.put(key, value);
    }
    public static String getToolArgs(String key) {
        Map<String, String> map = toolArgs.get();
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    public static void clear() {
        toolArgs.remove();
    }

}