package com.codehows.taelimbe.ai.chat.controller;

import com.codehows.taelimbe.ai.chat.dto.AiChatDTO;
import com.codehows.taelimbe.ai.chat.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.chat.service.AiChatService;
import com.codehows.taelimbe.ai.chat.service.ChatAgentService;
import com.codehows.taelimbe.ai.common.service.SseService;
import com.codehows.taelimbe.ai.embedding.service.EmbeddingService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * AI 채팅 관련 API를 제공하는 컨트롤러입니다.
 * 기존 AgentController와 AiChatController를 통합했습니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AiChatController {

    private final ChatAgentService chatAgentService;
    private final AiChatService aiChatService;
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

        chatAgentService.process(conversationId, req.getMessage(), user.userId());
        return emitter;
    }

    @PostMapping("/embeddings/reset")
    public CompletableFuture<ResponseEntity<String>> reset() {
        return embeddingService.reset()
                .thenApply(v -> ResponseEntity.ok("Embedding store reset and new text embedding process started successfully."))
                .exceptionally(ex -> {
                    log.error("resetAndEmbed 작업 실행 실패", ex);
                    Throwable cause = ex.getCause();
                    String errorMessage = (cause != null) ? cause.getMessage() : ex.getMessage();
                    return ResponseEntity.internalServerError().body("Failed to start reset and embedding process: " + errorMessage);
                });
    }

    // 사용자의 대화 목록 조회 (사이드바에서 사용)
    @GetMapping("/chat/history")
    public ResponseEntity<List<AiChatDTO>> getChatHistory(Authentication authentication) {
        List<AiChatDTO> chatList = aiChatService.getUserChatList(authentication);
        return ResponseEntity.ok(chatList);
    }


    // 특정 대화의 메시지 목록 조회
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<AiChatDTO>> getConversationMessages(
            @PathVariable String conversationId) {
        List<AiChatDTO> messages = aiChatService.getChatHistory(conversationId);
        return ResponseEntity.ok(messages);
    }


    // 새 채팅
    @PostMapping("/new/chat")
    public ResponseEntity<?> startNewChat(Authentication authentication) {
        String conversationId = aiChatService.startNewChat(authentication);
        return ResponseEntity.ok(Map.of("conversationId", conversationId));
    }
}
