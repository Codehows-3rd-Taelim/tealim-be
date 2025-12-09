package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiChatDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.AiChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/aiChat")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiChatService aiChatService;
    private final AgentService agentService;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<AiChatDTO>> getAllReports() {
        List<AiChatDTO> reports = aiChatService.getAllChats();
        return ResponseEntity.ok(reports);
    }

    // 새로 추가된 대화 시작/계속 엔드포인트
    /**
     * AI 에이전트와 대화를 시작하거나 기존 대화에 질문을 추가합니다.
     * 응답은 Server-Sent Events (SSE)를 통해 스트리밍됩니다.
     *
     * @param req 사용자의 메시지와 대화 ID(conversationId)를 포함하는 요청 DTO
     * @return SseEmitter 객체. 클라이언트에게 실시간 데이터를 스트리밍하는 데 사용됨
     */
    @PostMapping("/chat")
    public SseEmitter chat(@RequestBody ChatPromptRequest req) {
        log.info("채팅 요청 수신 - Message: {}, Conversation ID: {}", req.getMessage(), req.getConversationId());

        // AgentService의 chat 메서드를 호출하여 대화 처리를 위임하고 SSE Emitter를 반환
        return agentService.chat(req);
    }

}