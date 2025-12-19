package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.ai.service.AiReportService;
import com.codehows.taelimbe.ai.service.SseService;
import com.codehows.taelimbe.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ai/report")
public class AiReportController {

    private final AiReportService aiReportService;
    private final SseService sseService;

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

//    @PostMapping(
////            value = "/stream",
//            produces = MediaType.TEXT_EVENT_STREAM_VALUE
//    )
//    public SseEmitter generateReport(
//            @RequestBody ChatPromptRequest req,
//            Authentication authentication
//    ) {
//        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
//
//        String conversationId = UUID.randomUUID().toString();
//
//        // 1️⃣ SSE 연결 생성
//        SseEmitter emitter = sseService.createEmitter(conversationId);
//
//        // 2️⃣ 비동기 보고서 생성 시작
//        aiReportService.generateAsync(
//                conversationId,
//                req.getMessage(),
//                user
//        );
//
//        // 3️⃣ 즉시 emitter 반환
//        return emitter;
//    }

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

//    // 권한 기반 페이지네이션
//    @GetMapping
//    public ResponseEntity<Page<AiReportDTO>> getReportPage(
//            Authentication authentication,
//            @RequestParam int page,
//            @RequestParam int size,
//            @RequestParam(required = false) String searchText,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
//    ) {
//        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();
//        String keyword = (searchText == null || searchText.isBlank()) ? null : searchText;
//        Page<AiReportDTO> reportPage = aiReportService.getReportPage(user, page, size, keyword, startDate, endDate);
//        return ResponseEntity.ok(reportPage);
//    }
}
