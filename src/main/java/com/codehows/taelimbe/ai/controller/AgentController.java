package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.EmbedFileService;
import com.codehows.taelimbe.ai.service.SseService;
import com.codehows.taelimbe.ai.service.EmbeddingService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.UUID;


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



}

