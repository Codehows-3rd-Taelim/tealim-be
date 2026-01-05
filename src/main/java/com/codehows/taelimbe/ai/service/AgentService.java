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

        // 2) RAG 검색
        List<Content> contents = contentRetriever.retrieve(Query.from(message));

        if (contents.isEmpty()) {
            // 미답 질문 기록
            qnaService.recordQuestion(message);
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

                    aiChatService.saveAiMessage(conversationId, userId, rawAnswer);
                    sseService.complete(conversationId);

                    notificationService.notify(userId, NotificationType.AI_CHAT_SUCCESS, "AI 챗봇 답변이 도착했습니다");


                })
                .onError(e -> {
                    log.error("AI 스트림 오류", e);
                    sseService.completeWithError(conversationId, e);
                })
                .start();
    }

}