package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.EmbeddingRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.ai.service.SseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    // ConversationService를 주입받아 채팅 관련 비즈니스 로직을 위임합니다.
    private final AgentService agentService;
    // 임베딩 관련 비즈니스 로직을 처리하는 서비스를 주입받습니다.
    private final EmbeddingService embeddingService;
    private final SseService sseService;

    /**
     * 🔥 SSE 스트림 연결 (프론트 EventSource가 여기로 연결됨)
     */
    @GetMapping(value = "/stream/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String conversationId) {
        return sseService.createEmitter(conversationId);
    }

    /**
     * 🔥 메시지 전송 (SSE 스트림 반환 X, ID만 반환)
     */
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody ChatPromptRequest req) {

        // 대화 ID 생성 또는 기존 ID 유지
        String conversationId = req.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // AI 처리 비동기 실행
        agentService.process(conversationId, req.getMessage());

        // 프론트는 이 ID를 받아 SSE 연결
        return ResponseEntity.ok(conversationId);
    }



    @PostMapping(value = "/agent/report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report(
            @RequestBody ChatPromptRequest chatPromptRequest,
            HttpServletRequest request
    ) {
        // JWT Filter에서 저장된 userId 가져오기
        Long userId = Long.valueOf(request.getAttribute("userId").toString());

        // 채팅 요청 처리를 ConversationService로 위임합니다.
        return agentService.report(chatPromptRequest, userId);
    }
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
