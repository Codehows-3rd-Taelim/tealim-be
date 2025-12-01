package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;

    // 전체 리포트 조회 (권한에 따라 자동 필터링)
    @GetMapping("/aiReport")
    public ResponseEntity<List<AiReportDTO>> getAllReports() {
        List<AiReportDTO> reports = aiReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }
}