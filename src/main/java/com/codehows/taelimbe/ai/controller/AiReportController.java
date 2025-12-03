package com.codehows.taelimbe.ai.controller;

import com.codehows.taelimbe.ai.dto.AiReportDTO;
import com.codehows.taelimbe.ai.service.AiReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class AiReportController {

    private final AiReportService aiReportService;

    @GetMapping("/ai/Report")
    @ResponseBody
    public ResponseEntity<List<AiReportDTO>> getAllReports() {
        List<AiReportDTO> reports = aiReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }
}