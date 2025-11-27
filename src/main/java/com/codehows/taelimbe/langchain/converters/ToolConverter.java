package com.codehows.taelimbe.langchain.converters;

import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolParameters;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LangChain4j의 `ToolSpecification` 객체를 `com.google.genai` 라이브러리에서 사용하는 `Tool` 객체로 변환하는 유틸리티 클래스입니다.
 * Google Gemini API는 모델이 호출할 수 있는 도구들을 `FunctionDeclaration` 형태로 정의하여 받습니다.
 * 이 컨버터는 LangChain4j의 도구 명세를 Gemini의 함수 선언 형식에 맞게 매핑합니다.
 */
public class ToolConverter {

    /**
     * LangChain4j의 `ToolSpecification` 리스트를 `com.google.genai`의 `Tool` 리스트로 변환합니다.
     * Gemini API는 `Tool` 객체 내에 `FunctionDeclaration` 리스트를 포함하는 구조를 가집니다.
     *
     * @param toolSpecifications 변환할 LangChain4j `ToolSpecification` 리스트
     * @return 변환된 `com.google.genai` `Tool` 리스트. 도구 명세가 없으면 null을 반환합니다.
     */
    public static List<Tool> toGoogleAiTools(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null || toolSpecifications.isEmpty()) {
            return List.of(); // 도구 명세가 없으면 null을 반환하여 API 요청에서 도구 필드를 생략하도록 합니다.
        }
        return List.of(
                Tool.builder()
                        .functionDeclarations(toFunctionDeclarations(toolSpecifications)) // 도구 명세를 함수 선언으로 변환하여 포함
                        .build()
        );
    }

    /**
     * LangChain4j의 `ToolSpecification` 리스트를 `com.google.genai`의 `FunctionDeclaration` 리스트로 변환합니다.
     *
     * @param toolSpecifications 변환할 LangChain4j `ToolSpecification` 리스트
     * @return 변환된 `com.google.genai` `FunctionDeclaration` 리스트
     */
    private static List<FunctionDeclaration> toFunctionDeclarations(List<ToolSpecification> toolSpecifications) {
        return toolSpecifications.stream()
                .map(toolSpecification -> FunctionDeclaration.builder()
                        .name(toolSpecification.name()) // 도구의 이름을 설정합니다.
                        .description(toolSpecification.description()) // 도구의 설명을 설정합니다.
                        .parameters(toSchema(toolSpecification.parameters())) // 도구의 파라미터 스키마를 변환하여 설정합니다.
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * LangChain4j의 `ToolParameters` 객체를 `com.google.genai`의 `Schema` 객체로 변환합니다.
     * `ToolParameters`는 도구의 입력 파라미터에 대한 JSON 스키마 정의를 포함합니다.
     *
     * @param toolParameters 변환할 LangChain4j `ToolParameters`
     * @return 변환된 `com.google.genai` `Schema` 객체. 파라미터가 없으면 null을 반환합니다.
     */
    private static Schema toSchema(ToolParameters toolParameters) {
        if (toolParameters == null) {
            return null; // 파라미터가 없으면 null을 반환합니다.
        }

        // LangChain4j의 ToolParameters는 Map<String, Object> 형태의 JSON 스키마를 가집니다.
        // 이를 Gemini API의 Schema.Builder를 사용하여 구성합니다.
        Schema.Builder builder = Schema.builder();
        // 스키마의 'type' 필드를 설정합니다. 일반적으로 "object"입니다.
        builder.type(toolParameters.type().toUpperCase());

        // 스키마의 'properties' 필드를 설정합니다. 각 파라미터의 이름과 해당 스키마를 포함합니다.
        if (toolParameters.properties() != null && !toolParameters.properties().isEmpty()) {
            Map<String, Schema> propertiesMap = new HashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : toolParameters.properties().entrySet()) {
                // 각 파라미터의 스키마를 재귀적으로 변환하여 추가합니다.
                propertiesMap.put(entry.getKey(), mapToSchema(entry.getValue()));
            }
            builder.properties(propertiesMap);
        }

        // 스키마의 'required' 필드를 설정합니다. 필수 파라미터 목록입니다.
        if (toolParameters.required() != null && !toolParameters.required().isEmpty()) {
            builder.required(toolParameters.required());
        }
        
        return builder.build();
    }

    /**
     * Map 형태의 JSON 스키마를 `com.google.genai`의 `Schema` 객체로 재귀적으로 변환합니다.
     * 이 메서드는 `toSchema(ToolParameters)`에서 중첩된 스키마를 처리하기 위해 호출됩니다.
     *
     * @param map 스키마 정보를 담고 있는 Map
     * @return 변환된 `com.google.genai` `Schema` 객체
     */
    @SuppressWarnings("unchecked") // Map의 타입 캐스팅에 대한 경고를 억제합니다.
    private static Schema mapToSchema(Map<String, Object> map) {
        Schema.Builder builder = Schema.builder();
        if (map.containsKey("type")) {
            builder.type(map.get("type").toString().toUpperCase());
        }
        if (map.containsKey("description")) {
            builder.description(map.get("description").toString());
        }
        if (map.containsKey("enum")) {
            builder.enum_(((List<String>) map.get("enum")));
        }
        if (map.containsKey("properties")) {
            Map<String, Schema> propertiesMap = new HashMap<>();
            Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) map.get("properties");
            for (Map.Entry<String, Map<String, Object>> entry : properties.entrySet()) {
                propertiesMap.put(entry.getKey(), mapToSchema(entry.getValue()));
            }
            builder.properties(propertiesMap);
        }
        if (map.containsKey("required")) {
            builder.required(((List<String>) map.get("required")));
        }
        if (map.containsKey("items")) {
            builder.items(mapToSchema((Map<String, Object>) map.get("items")));
        }
        return builder.build();
    }
}
