package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.langchain.Agent;
import dev.langchain4j.service.TokenStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final Agent chatAgent;
    private final AiChatService aiChatService;
    private final SseService sseService;


    @Async
    public void process(String conversationId, String message, Long userId) {

        // 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        // 2차 답변 생성
        TokenStream stream = chatAgent.chat(message, conversationId);

        StringBuilder aiBuilder = new StringBuilder();

        stream.onPartialResponse(token -> {
                    aiBuilder.append(token);
                })
                .onCompleteResponse(finalResponse -> {
                    String rawAnswer = aiBuilder.toString();

                    aiChatService.saveAiMessage(
                            conversationId,
                            userId,
                            rawAnswer
                    );

                    sseService.sendFinalAndComplete(conversationId, rawAnswer);


                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                    sseService.completeWithError(conversationId, e);
                })
                .start();
    }

}
