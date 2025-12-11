package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.dto.ChatPromptRequest;
import com.codehows.taelimbe.ai.repository.AiReportMetaProjection;

import com.codehows.taelimbe.ai.repository.RawReportProjection;
import com.codehows.taelimbe.ai.service.AgentService;
import com.codehows.taelimbe.ai.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai/Report")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;
    private final AgentService agentService;
    @GetMapping("")
    @ResponseBody
    public ResponseEntity<List<AiReportDTO>> getAllReports() {
        List<AiReportMetaProjection> reportsProjection = aiReportService.getAllReports();

        List<AiReportDTO> reportsDTO = reportsProjection.stream()
                .map(AiReportDTO::fromProjection)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reportsDTO);
    }


    @GetMapping("/{reportId}/rawReport")
    @ResponseBody
    public ResponseEntity<RawReportProjection> getRawReport(@PathVariable Long reportId) {
        RawReportProjection rawReportProjection = aiReportService.getRawReport(reportId);
        return ResponseEntity.ok(rawReportProjection);
    }


    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE) // 경로를 명확히 /ai/Report/sse로 설정
    public SseEmitter report(
            @RequestParam("message") String message,
            @RequestParam("conversationId") String conversationId
    ) {
        ChatPromptRequest request = new ChatPromptRequest(message, conversationId);
        return agentService.report(request);
    }
}