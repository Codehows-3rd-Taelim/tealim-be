package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;

import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.ai.service.AiReportService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/report")
public class AiReportController {

    private final AiReportService aiReportService;

    // 보고서 목록 조회
    @GetMapping
    public ResponseEntity<List<AiReportDTO>> getAllReports(
            Authentication authentication
    ) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        return ResponseEntity.ok(
                aiReportService.getAllReports(user)
        );
    }

    // 1단계: 보고서 생성 시작
    @PostMapping
    public ResponseEntity<Map<String, String>> createReport(
            @RequestBody ChatPromptRequest req,
            Authentication authentication
    ) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        String conversationId =
                aiReportService.startGenerateReport(req, user);

        return ResponseEntity.ok(
                Map.of("conversationId", conversationId)
        );
    }

    // 2단계: SSE 스트림 구독
    @GetMapping(
            value = "/stream/{conversationId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(@PathVariable String conversationId) {
        return aiReportService.connectSse(conversationId);
    }

    /**
     * raw 리포트 조회
     */
    @GetMapping("/{reportId}/rawReport")
    public ResponseEntity<RawReportProjection> getRawReport(
            @PathVariable Long reportId
    ) {
        return ResponseEntity.ok(
                aiReportService.getRawReport(reportId)
        );
    }

}