package com.codehows.taelimbe.ai.report.controller;

import com.codehows.taelimbe.ai.chat.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.report.dto.AiReportDTO;
import com.codehows.taelimbe.ai.report.repository.RawReportProjection;
import com.codehows.taelimbe.ai.report.service.AiReportService;
import com.codehows.taelimbe.ai.common.service.SseService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/report")
public class AiReportController {

    private final AiReportService aiReportService;
    private final SseService sseService;

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

    // 보고서 생성 시작
    @PostMapping
    public ResponseEntity<Void> createReport(
            @RequestParam String conversationId,
            @RequestBody ChatPromptRequest req,
            Authentication authentication
    ) {
        UserPrincipal user =
                (UserPrincipal) authentication.getPrincipal();

        aiReportService.startGenerateReport(conversationId, req, user);

        return ResponseEntity.ok().build();
    }

    // 2단계: SSE 스트림 구독
    @GetMapping(
            value = "/stream/{conversationId}",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(
            @PathVariable String conversationId,
            Authentication authentication
    ) {
        return sseService.createEmitter(conversationId);
    }

    // Raw 보고서 조회
    @GetMapping("/{reportId}/rawReport")
    public ResponseEntity<RawReportProjection> getRawReport(@PathVariable Long reportId) {
        return ResponseEntity.ok(aiReportService.getRawReport(reportId));
    }

    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(
            @PathVariable Long reportId,
            Authentication authentication
    ) {
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
        aiReportService.deleteReport(reportId, user);
        return ResponseEntity.ok().build();
    }
}
