package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.EmbeddingRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.SseService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")   // ✔ 너 원래 코드 그대로
public class AgentController {

    private final AgentService agentService;
    private final SseService sseService;
    private final EmbeddingService embeddingService;

    /**
     * ------------------------------------------------------------
     * 1) 메시지 전송 (SSE emitter 반환 X)
     * POST /api/agent/chat
     * ------------------------------------------------------------
     */
//    @PostMapping("/agent/chat")
//    public ResponseEntity<String> chat(
//            @RequestBody ChatPromptRequest req,
//            HttpServletRequest request
//    ) {
//        Long userId = Long.valueOf(request.getAttribute("userId").toString());
//
//        String conversationId = req.getConversationId();
//        if (conversationId == null || conversationId.isBlank()) {
//            conversationId = UUID.randomUUID().toString();
//        }
//
//        agentService.process(conversationId, req.getMessage(), userId);
//
//        return ResponseEntity.ok(conversationId);
//    }
    @PostMapping("/agent/chat")
    public ResponseEntity<String> chat(
            @RequestBody ChatPromptRequest req,
            HttpServletRequest request
    ) {

        // ★ 추가된 로그 — 너 코드 그대로 유지
        log.info("🔍 [chat] request.getAttribute(\"userId\") = {}", request.getAttribute("userId"));
        log.info("🔍 [chat] req.getConversationId() = {}", req.getConversationId());
        log.info("🔍 [chat] req.getMessage() = {}", req.getMessage());

        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        log.info("🔍 [chat] parsed userId = {}", userId);

        String conversationId = req.getConversationId();
        log.info("🔍 [chat] incoming conversationId = {}", conversationId);
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();

            // ★ 여기만 추가
            log.info("🔍 [chat] generated new conversationId = {}", conversationId);
        }
        log.info("🔍 [chat] calling agentService.process()");
        agentService.process(conversationId, req.getMessage(), userId);

        return ResponseEntity.ok(conversationId);
    }


    /**
     * ------------------------------------------------------------
     * 2) SSE 연결
     * GET /api/agent/stream/{conversationId}
     * ------------------------------------------------------------
     */
    @GetMapping(value = "/agent/stream/{conversationId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(@PathVariable String conversationId) {
        return sseService.createEmitter(conversationId);
    }

    /**
     * ------------------------------------------------------------
     * 3) 리포트 생성 (SSE 직접 반환)
     * POST /api/agent/report
     * ------------------------------------------------------------
     */
    @PostMapping(value = "/agent/report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report(
            @RequestBody ChatPromptRequest req,
            HttpServletRequest request
    ) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
        return agentService.report(req, userId);
    }

    /**
     * ------------------------------------------------------------
     * 4) Embedding 처리
     * POST /api/embeddings
     * ------------------------------------------------------------
     */
    @PostMapping("/embeddings")
    public CompletableFuture<ResponseEntity<String>> embed(
            @RequestBody EmbeddingRequest request
    ) {
        return embeddingService.embedAndStore(request.getText())
                .thenApply(v -> ResponseEntity.ok("Embedding started"))
                .exceptionally(ex -> {
                    log.error("embed error", ex);
                    return ResponseEntity.internalServerError().body(ex.getMessage());
                });
    }

    /**
     * ------------------------------------------------------------
     * 5) Embedding 리셋
     * POST /api/embeddings/reset
     * ------------------------------------------------------------
     */
    @PostMapping("/embeddings/reset")
    public CompletableFuture<ResponseEntity<String>> resetAndEmbed(
            @RequestBody EmbeddingRequest request
    ) {
        return embeddingService.resetAndEmbed(request.getText())
                .thenApply(v -> ResponseEntity.ok("Reset + embedding started"))
                .exceptionally(ex -> {
                    log.error("reset embed error", ex);
                    return ResponseEntity.internalServerError().body(ex.getMessage());
                });
    }
}
