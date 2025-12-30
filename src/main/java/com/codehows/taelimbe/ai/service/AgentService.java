package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.langchain.Agent;
import com.codehows.taelimbe.langchain.embaddings.EmbeddingStoreManager;
import com.codehows.taelimbe.notification.constant.NotificationType;
import com.codehows.taelimbe.notification.service.NotificationService;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 에이전트와의 대화 로직을 캡슐화하는 서비스 클래스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {
    private final SseService sseService;
    private final AiChatService aiChatService;
    private final NotificationService notificationService;
    private final EmbeddingService embeddingService;
    private final QnaService qnaService;
    private final ContentRetriever contentRetriever;

    @Qualifier("chatAgent")
    private final Agent chatAgent;

    @Async
    public void process(String conversationId, String message, Long userId) {

        // 1) 사용자 메시지 저장
        aiChatService.saveUserMessage(conversationId, userId, message);

        List<Content> contents = contentRetriever.retrieve(Query.from(message));

        if (contents.isEmpty()) {

            String fallback = "답변할 수 없는 정보입니다.";

            aiChatService.saveAiMessage(conversationId, userId, fallback);
            sseService.send(conversationId, fallback);
            sseService.complete(conversationId);

            // 미해결 질문 기록
            qnaService.recordQuestion(message);

            notificationService.notify(
                    userId,
                    NotificationType.AI_CHAT_SUCCESS,
                    "AI 챗봇 답변이 도착했습니다"
            );

            return;
        }



    // 2) TokenStream 가져오기
        TokenStream stream = chatAgent.chat(message, conversationId);

        StringBuilder aiBuilder = new StringBuilder();

        // 3) 토큰 스트리밍 시작
        stream.onNext(token -> {
                    aiBuilder.append(token);
                    sseService.send(conversationId, token);
                })
                .onComplete(finalResponse -> {
                    String rawAnswer = aiBuilder.toString();
                    String normalizedAnswer = normalizeForChat(rawAnswer);


                    aiChatService.saveAiMessage(conversationId, userId, normalizedAnswer);
                    sseService.complete(conversationId);

                    // 답변 불가인 정보 미답 질문에 저장
                    if ("답변드릴 수 없는 정보입니다.".equals(rawAnswer)) {
                        qnaService.recordQuestion(message);
                    }

                    notificationService.notify(userId, NotificationType.AI_CHAT_SUCCESS, "AI 챗봇 답변이 도착했습니다");


                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                    sseService.completeWithError(conversationId, e);
                })
                .start();  
    }

    private String normalizeForChat(String text) {
        if (text == null) return null;

        // 마크다운/기호 제거
        String t = text
                .replace("**", "")
                .replace("*", "")
                .replaceAll("(?m)^#+\\s*", "")
                .replaceAll("(?m)^-+\\s*", "");

        // 줄 단위 처리
        String[] lines = t.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            // 값 없는 헤더 제거 (":"로 끝나고 다음 줄이 또 라벨:값 형태)
            if (line.endsWith(":") &&
                    i + 1 < lines.length &&
                    lines[i + 1].contains(":")) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(line);
        }

        // 공백 정리
        return result.toString()
                .replaceAll("\\s{2,}", " ")
                .trim();
    }




}