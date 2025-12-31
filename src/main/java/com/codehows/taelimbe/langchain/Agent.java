package com.codehows.taelimbe.langchain;

import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;

/**
 * LangChain4j의 AI 서비스를 정의하는 인터페이스입니다.
 * 이 인터페이스를 통해 AI 모델과 상호작용하며, 대화 기록 관리, 시스템 메시지 설정, 스트리밍 응답 등을 처리합니다.
 *
 * `@AiService` 어노테이션을 통해 LangChain4j가 런타임에 이 인터페이스의 구현체를 자동으로 생성합니다.
 */
@AiService
public interface Agent {

    /**
     * AI 에이전트의 시스템 메시지를 정의합니다.
     * 이 메시지는 AI의 역할, 지시사항, 제약 조건 등을 설정하여 AI의 행동을 안내합니다.
     * 여기서는 AI가 "대화할 수 있고 도구를 사용하여 질문에 답할 수 있는 유용한 어시스턴트"임을 명시합니다.
     */
    @SystemMessage("""
            당신은 사용자의 문제 해결을 돕는 AI입니다.
            확실하지 않은 정보이거나 확인 가능한 자료에 명시적으로 없는 내용에 대해서는
            단정하지 말고, "현재 확인 가능한 정보 기준으로는" 이렇게 표현을 사용하세요.
            원인이나 해결 방법을 제시할 수 없는 경우에는 간단히 한계를 밝힌 뒤
            필요한 정보 이외에 다른 정보에 대한 언급은 하지말고
            사용자가 일반적으로 다음에 취할 수 있는 행동을 안내하세요.
            추측이 필요한 경우에는 그 사실을 명확히 밝히세요.
            """)
    /**
     * 사용자의 메시지를 받아 스트리밍 방식으로 AI의 응답을 반환하는 메서드입니다.
     *
     * @param message 사용자의 입력 메시지입니다. `@UserMessage` 어노테이션을 통해
     *                이 파라미터가 AI에게 전달될 사용자 메시지임을 나타냅니다.
     * @param memoryId 각 대화를 고유하게 식별하는 ID입니다. `@MemoryId` 어노테이션을 통해 LangChain4j가
     *                 이 ID를 사용하여 해당 대화의 기록(ChatMemory)을 관리하도록 합니다.
     *                 이를 통해 멀티턴 대화가 가능해집니다.
     * @return AI의 응답을 담은 토큰 스트림(`TokenStream`)을 반환합니다.
     *         `TokenStream`을 사용하면 AI의 응답을 실시간으로 토큰 단위로 받을 수 있습니다.
     */
    TokenStream chat(@UserMessage String message, @MemoryId Object memoryId);
}
