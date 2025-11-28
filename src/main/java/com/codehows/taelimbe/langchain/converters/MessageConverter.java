package com.codehows.taelimbe.langchain.converters;

import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.langchain4j.data.message.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LangChain4j의 ChatMessage를 `com.google.genai`의 Content 객체로 변환하는 유틸리티 클래스입니다.
 */
@Slf4j
public class MessageConverter {
    /**
     * ChatMessage 리스트를 Content 리스트로 변환합니다.
     * @param messages LangChain4j의 ChatMessage 리스트
     * @return `com.google.genai`의 Content 리스트
     */
    public static List<Content> toGoogleAiMessages(List<ChatMessage> messages) {
        return messages.stream()
                .map(MessageConverter::toGoogleAiMessage)
                .collect(Collectors.toList());
    }

    /**
     * 단일 ChatMessage를 Content 객체로 변환합니다.
     * @param message LangChain4j의 ChatMessage
     * @return `com.google.genai`의 Content 객체
     */
    private static Content toGoogleAiMessage(ChatMessage message) {
        String role = toGoogleAiRole(message);
        List<Part> parts = toGoogleAiParts(message);

        return Content.builder()
                .role(role)
                .parts(parts)
                .build();
    }

    /**
     * ChatMessage의 타입에 따라 `com.google.genai`의 역할(role)을 결정합니다.
     * @param message LangChain4j의 ChatMessage
     * @return `com.google.genai`의 역할 문자열 ("user" 또는 "model")
     */
    private static String toGoogleAiRole(ChatMessage message) {
        if (message instanceof UserMessage || message instanceof ToolExecutionResultMessage) {
            return "user";
        } else if (message instanceof AiMessage || message instanceof SystemMessage) {
            return "model";
        } else {
            throw new IllegalArgumentException("Unknown message type: " + message);
        }
    }

    /**
     * ChatMessage의 내용을 `com.google.genai`의 Part 리스트로 변환합니다.
     * @param message LangChain4j의 ChatMessage
     * @return `com.google.genai`의 Part 리스트
     */
    private static List<Part> toGoogleAiParts(ChatMessage message) {
        if (message instanceof UserMessage) {
            return Collections.singletonList(Part.fromText(((UserMessage) message).singleText()));
        }

        if (message instanceof SystemMessage) {
            // SystemMessage는 UserMessage처럼 텍스트로 처리
            return Collections.singletonList(Part.fromText(((SystemMessage) message).text()));
        }

        if (message instanceof AiMessage aiMessage) {
            if (aiMessage.hasToolExecutionRequests()) {
                List<Part> parts = aiMessage.toolExecutionRequests().stream()
                        .map(toolExecutionRequest -> {
                            try {
                                String argumentsJson = toolExecutionRequest.arguments();
                                JsonObject jsonObject = JsonParser.parseString(argumentsJson).getAsJsonObject();
                                return Part.fromFunctionCall(
                                        toolExecutionRequest.name(),
                                        JsonConverter.jsonToMap(jsonObject));
                            } catch (Exception e) {
                                // 로그 출력 및 기본 fallback 처리
                                log.error("Invalid JSON in toolExecutionRequest arguments: {}", toolExecutionRequest.arguments(), e);
                                return Part.fromFunctionCall(toolExecutionRequest.name(), Collections.emptyMap());
                            }
                        })
                        .collect(Collectors.toList());
                // 텍스트가 있는 경우 텍스트 파트도 추가
                if (aiMessage.text() != null && !aiMessage.text().isEmpty()) {
                    parts.addFirst(Part.fromText(aiMessage.text()));
                }
                return parts;
            } else {
                return Collections.singletonList(Part.fromText(aiMessage.text()));
            }
        }

        if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            return Collections.singletonList(
                    Part.fromFunctionResponse(
                            toolExecutionResultMessage.toolName(),
                            Collections.singletonMap("content", toolExecutionResultMessage.text()))
            );
        }

        throw new IllegalArgumentException("Unknown message type: " + message);
    }
}

