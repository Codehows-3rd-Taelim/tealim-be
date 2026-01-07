package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.langchain.Agent;
import com.codehows.taelimbe.langchain.JudgeAgent;
import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.service.NotificationService;
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
    private final JudgeAgent judgeAgent;

    private final AiChatService aiChatService;
    private final QnaService qnaService;
    private final SseService sseService;
    private final NotificationService notificationService;

    @Async
    public void process(String conversationId, String message, Long userId) {

        // 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        // 1차 판단 (JudgeAgent가 RAG 포함해서 판단)
        boolean canAnswer = judge(message);

        // 미답 질문 기록
        if (!canAnswer) {
            qnaService.recordQuestion(message);
        }

        // 2차 답변 생성
        TokenStream stream = chatAgent.chat(message, conversationId);

        StringBuilder aiBuilder = new StringBuilder();

        stream.onNext(token -> {
                    aiBuilder.append(token);
                })
                .onComplete(finalResponse -> {
                    String rawAnswer = aiBuilder.toString();

                    aiChatService.saveAiMessage(
                            conversationId,
                            userId,
                            rawAnswer
                    );

                    sseService.sendFinalAndComplete(conversationId, rawAnswer);

                    notificationService.notify(
                            userId,
                            NotificationType.AI_CHAT_SUCCESS,
                            "AI 챗봇 답변이 도착했습니다"
                    );
                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                    sseService.completeWithError(conversationId, e);
                })
                .start();
    }
    /**
     * JudgeAgent를 이용한 YES / NO 판단
     * - contentRetriever는 JudgeAgent builder에 연결돼 있음
     */
    private boolean judge(String message) {
        StringBuilder sb = new StringBuilder();

        try {
            judgeAgent.judge(message)
                    .onNext(sb::append)
                    .onError(e -> {
                        throw new RuntimeException(e);
                    })
                    .start();

            String result = sb.toString().trim();
            log.info("[Judge] {}", result);

            return "YES".equalsIgnoreCase(result);
        } catch (Exception e) {
            log.error("Judge 실패 → NO 처리", e);
            return false;
        }
    }
}
