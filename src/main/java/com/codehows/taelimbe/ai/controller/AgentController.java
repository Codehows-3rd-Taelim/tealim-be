package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.EmbeddingRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.CompletableFuture;

/**
 * AI 에이전트와의 대화 및 임베딩 관리를 처리하는 API 컨트롤러입니다.
 * `@RestController`는 이 클래스가 RESTful 웹 서비스의 컨트롤러임을 나타내며,
 * 모든 메서드의 반환 값이 HTTP 응답 본문으로 직접 직렬화됨을 의미합니다.
 * `@RequestMapping("/api")`는 이 컨트롤러의 모든 핸들러 메서드가 "/api" 경로 아래에 매핑됨을 지정합니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 * `@Slf4j`는 Lombok 어노테이션으로, 로깅을 위한 `log` 객체를 자동으로 생성합니다.
 */
@RestController
@RequestMapping("")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    // ConversationService를 주입받아 채팅 관련 비즈니스 로직을 위임합니다.
    private final AgentService agentService;
    // 임베딩 관련 비즈니스 로직을 처리하는 서비스를 주입받습니다.
    private final EmbeddingService embeddingService;

    /**
     * 사용자의 메시지를 받아 AI와 대화하고, 응답을 스트리밍으로 반환합니다.
     * `/agent/chat`은 일반적인 대화를, `/agent/report`는 보고서 생성을 위한 특정 프롬프트 처리를 담당합니다.
     * 이 메서드는 실제 채팅 로직을 `conversationService`로 위임합니다.
     *
     * @param chatPromptRequest 사용자 메시지와 대화 ID를 포함하는 요청 DTO
     * @return Server-Sent Events (SSE)를 통해 AI의 응답을 스트리밍하는 SseEmitter
     */
    @PostMapping(value = "/agent/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(
            @RequestBody ChatPromptRequest chatPromptRequest
    ) {
        return agentService.chat(chatPromptRequest);
    }

//    @PostMapping(value = "/agent/report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter report(
//            @RequestBody ChatPromptRequest chatPromptRequest
//    ) {
//        // 채팅 요청 처리를 ConversationService로 위임합니다.
//        return agentService.report(chatPromptRequest);
//    }

    /**
     * 주어진 텍스트를 비동기적으로 임베딩하여 벡터 저장소에 저장합니다.
     * 이 메서드는 요청을 즉시 수락하고 백그라운드에서 작업을 처리합니다.
     *
     * @param request 임베딩할 텍스트를 포함하는 요청 DTO
     * @return 작업의 비동기 실행 결과를 담은 CompletableFuture<ResponseEntity>
     */
    @PostMapping("/embeddings")
    public CompletableFuture<ResponseEntity<String>> embed(@RequestBody EmbeddingRequest request) { // @Valid 추가
        return embeddingService.embedAndStore(request.getText())
                // 임베딩 및 저장 작업이 성공적으로 시작되면 200 OK 응답을 반환합니다.
                .thenApply(v -> ResponseEntity.ok("Text embedding and storing process started successfully."))
                // 작업 중 예외 발생 시 500 Internal Server Error 응답을 반환합니다.
                .exceptionally(ex -> {
                    log.error("embedAndStore 작업 실행 실패", ex);
                    Throwable cause = ex.getCause();
                    String errorMessage = (cause != null) ? cause.getMessage() : ex.getMessage();
                    return ResponseEntity.internalServerError().body("Failed to start embedding process: " + errorMessage);
                });
    }

    /**
     * 기존 임베딩을 모두 삭제하고, 주어진 텍스트로 벡터 저장소를 비동기적으로 재설정합니다.
     * 이 메서드는 요청을 즉시 수락하고 백그라운드에서 작업을 처리합니다.
     *
     * @param request 재설정에 사용할 새로운 텍스트를 포함하는 요청 DTO
     * @return 작업의 비동기 실행 결과를 담은 CompletableFuture<ResponseEntity>
     */
    @PostMapping("/embeddings/reset")
    public CompletableFuture<ResponseEntity<String>> resetAndEmbed(@RequestBody EmbeddingRequest request) { // @Valid 추가
        return embeddingService.resetAndEmbed(request.getText())
                // 저장소 재설정 및 임베딩 작업이 성공적으로 시작되면 200 OK 응답을 반환합니다.
                .thenApply(v -> ResponseEntity.ok("Embedding store reset and new text embedding process started successfully."))
                // 작업 중 예외 발생 시 500 Internal Server Error 응답을 반환합니다.
                .exceptionally(ex -> {
                    log.error("resetAndEmbed 작업 실행 실패", ex);
                    Throwable cause = ex.getCause();
                    String errorMessage = (cause != null) ? cause.getMessage() : ex.getMessage();
                    return ResponseEntity.internalServerError().body("Failed to start reset and embedding process: " + errorMessage);
                });
    }
}
