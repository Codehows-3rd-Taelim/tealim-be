package com.codehows.taelimbe.langchain.converters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON 객체와 Java 컬렉션 간의 변환을 담당하는 유틸리티 클래스입니다.
 * Google의 GSON 라이브러리를 사용하여 JSON 직렬화/역직렬화를 수행합니다.
 */
public class JsonConverter {

    /**
     * GSON `JsonObject`를 `Map<String, Object>`로 변환합니다.
     * JSON 객체의 키-값 쌍을 Java Map으로 매핑합니다.
     *
     * @param jsonObject 변환할 `JsonObject`
     * @return 변환된 `Map<String, Object>`
     */
    public static Map<String, Object> jsonToMap(JsonObject jsonObject) {
        Map<String, Object> map = new HashMap<>();
        // JsonObject의 모든 엔트리를 순회하며 Map에 추가합니다.
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            map.put(entry.getKey(), jsonElementToObject(entry.getValue()));
        }
        return map;
    }

    /**
     * GSON `JsonArray`를 `List<Object>`로 변환합니다.
     * JSON 배열의 요소들을 Java List로 매핑합니다.
     *
     * @param jsonArray 변환할 `JsonArray`
     * @return 변환된 `List<Object>`
     */
    public static List<Object> jsonToList(JsonArray jsonArray) {
        List<Object> list = new ArrayList<>();
        // JsonArray의 모든 요소를 순회하며 List에 추가합니다.
        for (JsonElement element : jsonArray) {
            list.add(jsonElementToObject(element));
        }
        return list;
    }

    /**
     * GSON `JsonElement`를 적절한 Java 객체(Map, List, Primitive 타입)로 변환합니다.
     * 이 메서드는 재귀적으로 호출되어 중첩된 JSON 구조를 처리할 수 있습니다.
     *
     * @param jsonElement 변환할 `JsonElement`
     * @return 변환된 Java 객체 (Map, List, Boolean, Number, String 또는 null)
     * @throws IllegalArgumentException 지원하지 않는 `JsonElement` 타입인 경우 발생
     */
    private static Object jsonElementToObject(JsonElement jsonElement) {
        if (jsonElement.isJsonObject()) {
            // JsonObject인 경우 Map으로 변환합니다.
            return jsonToMap(jsonElement.getAsJsonObject());
        } else if (jsonElement.isJsonArray()) {
            // JsonArray인 경우 List로 변환합니다.
            return jsonToList(jsonElement.getAsJsonArray());
        } else if (jsonElement.isJsonPrimitive()) {
            // JsonPrimitive인 경우 기본 타입(Boolean, Number, String)으로 변환합니다.
            JsonPrimitive primitive = jsonElement.getAsJsonPrimitive();
            if (primitive.isBoolean()) {
                return primitive.getAsBoolean();
            } else if (primitive.isNumber()) {
                return primitive.getAsNumber();
            } else {
                return primitive.getAsString();
            }
        } else if (jsonElement.isJsonNull()) {
            // JsonNull인 경우 null을 반환합니다.
            return null;
        } else {
            // 그 외의 알 수 없는 타입인 경우 예외를 발생시킵니다.
            throw new IllegalArgumentException("Unsupported JsonElement type: " + jsonElement);
        }
    }
}
