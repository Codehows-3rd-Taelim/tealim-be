package com.codehows.taelimbe.ai.constant;

/**
 * AI 채팅에서 메시지를 보낸 주체를 나타내는 Enum
 * USER: 실제 사용자(Role.USER, Role.MANAGER, Role.ADMIN 모두 포함)가 보낸 메시지
 * AI: AI 에이전트(Gemini)가 보낸 응답
 */
public enum SenderType {
    USER("사용자"),
    AI("AI");

    private final String description;

    SenderType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}