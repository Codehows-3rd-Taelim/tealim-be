package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;


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
