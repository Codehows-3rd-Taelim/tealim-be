package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    /**
     * 사용자의 대화 목록 조회 (사이드바에서 사용)
     * 각 대화의 첫 번째 메시지 (사용자 질문)를 제목으로 반환
     */
    @GetMapping("/chat-history")
    public ResponseEntity<List<AiChatDTO>> getChatHistory() {
        List<AiChatDTO> chatList = aiChatService.getUserChatList();
        return ResponseEntity.ok(chatList);
    }

    /**
     * 특정 대화의 메시지 목록 조회
     */
    @GetMapping("/conversation/{conversationId}")
    public ResponseEntity<List<AiChatDTO>> getConversationMessages(
            @PathVariable String conversationId) {
        List<AiChatDTO> messages = aiChatService.getChatHistory(conversationId);
        return ResponseEntity.ok(messages);
    }

    /**
     * 대화 삭제
     */
    @DeleteMapping("/conversation/{conversationId}")
    public ResponseEntity<String> deleteConversation(
            @PathVariable String conversationId) {
        try {
            aiChatService.deleteConversation(conversationId);
            return ResponseEntity.ok("대화가 삭제되었습니다.");
        } catch (Exception e) {
            log.error("대화 삭제 실패: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body("대화 삭제 중 오류가 발생했습니다.");
        }
    }
}
