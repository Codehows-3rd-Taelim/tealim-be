package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.dto.EmbeddingRequest;
import com.codehows.taelimbe.ai.dto.QnaEmbeddingRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import com.codehows.taelimbe.ai.service.SseService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentController {

    private final AgentService agentService;
    private final SseService sseService;
    private final EmbeddingService embeddingService;
    private final EmbedFileService embedFileService;

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


    /**
     * ------------------------------------------------------------
     * 4) Embedding 처리
     * POST /api/embeddings
     * ------------------------------------------------------------
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
     * ------------------------------------------------------------
     * 5) Embedding 리셋
     * POST /api/embeddings/reset
     * ------------------------------------------------------------
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



//    @PostMapping("/embeddings/add")
//    public CompletableFuture<ResponseEntity<String>> addEmbed(@RequestBody EmbeddingRequest request) {
//        return embeddingService.embedByValue(request.getText())
//                // 저장소 재설정 및 임베딩 작업이 성공적으로 시작되면 200 OK 응답을 반환합니다.
//                .thenApply(v -> ResponseEntity.ok("success"))
//                // 작업 중 예외 발생 시 500 Internal Server Error 응답을 반환합니다.
//                .exceptionally(ex -> {
//                    log.error("resetAndEmbed 작업 실행 실패", ex);
//                    Throwable cause = ex.getCause();
//                    String errorMessage = (cause != null) ? cause.getMessage() : ex.getMessage();
//                    return ResponseEntity.internalServerError().body("Failed to start reset and embedding process: " + errorMessage);
//                });
//    }

//    @DeleteMapping("/embeddings/{key}")
//    public CompletableFuture<ResponseEntity<String>> deleteEmbed(@PathVariable String key) {
//        return embeddingService.deleteByKey(key)
//                // 저장소 재설정 및 임베딩 작업이 성공적으로 시작되면 200 OK 응답을 반환합니다.
//                .thenApply(v -> ResponseEntity.ok("success"))
//                // 작업 중 예외 발생 시 500 Internal Server Error 응답을 반환합니다.
//                .exceptionally(ex -> {
//                    log.error("resetAndEmbed 작업 실행 실패", ex);
//                    Throwable cause = ex.getCause();
//                    String errorMessage = (cause != null) ? cause.getMessage() : ex.getMessage();
//                    return ResponseEntity.internalServerError().body("Failed to start reset and embedding process: " + errorMessage);
//                });
//    }

//    @PostMapping("/embeddings/upload-csv")
//    public CompletableFuture<ResponseEntity<String>> embedCsv(@RequestParam("file") MultipartFile file) {
//        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
//            return CompletableFuture.completedFuture(
//                    ResponseEntity.badRequest().body("CSV 파일을 선택하거나 유효한 파일 형식을 확인해주세요."));
//        }

//    @PostMapping("/embeddings/upload-csv")
//    public CompletableFuture<ResponseEntity<String>> embedCsv(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("embedKey") String embedKey
//    ) {
//        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
//            return CompletableFuture.completedFuture(
//                    ResponseEntity.badRequest().body("CSV 파일을 선택하거나 유효한 파일 형식을 확인해주세요.")
//            );
//        }
//
//        return embeddingService.embedAndStoreCsv(file, embedKey)
//                .thenApply(v -> ResponseEntity.ok("CSV 파일 임베딩 시작됨"))
//                .exceptionally(ex -> {
//                    log.error("CSV embed error", ex);
//                    return ResponseEntity.internalServerError().body(ex.getMessage());
//                });
//    }
//
//
//    @PostMapping("/embeddings/upload-pdf")
//    public CompletableFuture<ResponseEntity<String>> embedPdf(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam("embedKey") String embedKey
//    ) {
//        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
//            return CompletableFuture.completedFuture(
//                    ResponseEntity.badRequest().body("PDF 파일을 선택해주세요.")
//            );
//        }
//
//        return embeddingService.embedAndStorePdf(file, embedKey)
//                .thenApply(v -> ResponseEntity.ok("PDF 병렬 임베딩 시작됨"))
//                .exceptionally(ex -> {
//                    log.error("PDF embed error", ex);
//                    return ResponseEntity.internalServerError().body(ex.getMessage());
//                });
//    }


//    @PostMapping("/embeddings/reset-only")
//    public ResponseEntity<Void> resetOnly() {
//        embeddingService.resetOnly();
//        return ResponseEntity.ok().build();
//    }


    }

