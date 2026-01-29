package com.codehows.taelimbe.ai.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 프롬프트 요청을 위한 데이터 전송 객체(DTO)입니다.
 * 클라이언트로부터 AI 에이전트에게 전달될 메시지와 대화 ID를 캡슐화합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatPromptRequest {
    /**
     * 사용자로부터 AI 에이전트에게 전달될 메시지입니다.
     */
    private String message;
    /**
     * 현재 대화의 고유 ID입니다.
     * 이 ID를 통해 AI 에이전트가 이전 대화의 컨텍스트를 유지할 수 있습니다.
     */
    private String conversationId;
}
