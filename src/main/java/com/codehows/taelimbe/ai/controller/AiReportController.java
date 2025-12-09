package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.aiReport.AiReportDTO;
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
@RequestMapping("")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;
    private final AgentService agentService;
    @GetMapping("/ai/Report")
    @ResponseBody
    public ResponseEntity<List<AiReportDTO>> getAllReports() {
        List<AiReportMetaProjection> reportsProjection = aiReportService.getAllReports();

        List<AiReportDTO> reportsDTO = reportsProjection.stream()
                .map(AiReportDTO::fromProjection)
                .collect(Collectors.toList());
        return ResponseEntity.ok(reportsDTO);
    }


    @GetMapping("/ai/Report/{reportId}/rawReport")
    @ResponseBody
    public ResponseEntity<RawReportProjection> getrawReport(@PathVariable Long reportId) {
        RawReportProjection rawReportProjection = aiReportService.getrawReport(reportId);
        return ResponseEntity.ok(rawReportProjection);
    }


    @PostMapping(value = "/ai/Report", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter report(
            @RequestBody ChatPromptRequest chatPromptRequest
    ) {
        return agentService.report(chatPromptRequest);
    }
}