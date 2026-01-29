package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import com.codehows.taelimbe.ai.service.SseService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final SseService sseService;
    private final EmbeddingService embeddingService;

    // SSE 연결, AI 응답 받아옴
    @PostMapping("/agent/chat")
    public SseEmitter chat(@RequestBody ChatPromptRequest req, Authentication authentication) {

        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        String conversationId = req.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        SseEmitter emitter = sseService.createEmitter(conversationId);

        agentService.process(conversationId, req.getMessage(), user.userId());
        return emitter;
    }

    @PostMapping("/embeddings/reset")
    public CompletableFuture<ResponseEntity<String>> reset() { // @Valid 추가
        return embeddingService.reset()
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

