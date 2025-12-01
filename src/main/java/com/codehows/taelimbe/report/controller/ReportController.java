package com.codehows.taelimbe.report.controller;

import com.codehows.taelimbe.report.dto.ReportSyncRequestDTO;
import com.codehows.taelimbe.report.dto.ReportDetailRequestDTO;
import com.codehows.taelimbe.report.dto.ReportDTO;
import com.codehows.taelimbe.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncReports(@Valid @RequestBody ReportSyncRequestDTO req) {
        int count = reportService.syncReports(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    @PostMapping("/save-detail")
    public ResponseEntity<ReportDTO> saveDetail(@Valid @RequestBody ReportDetailRequestDTO req) {
        return ResponseEntity.ok(reportService.saveDetail(req));
    }

    @GetMapping("/list")
    public ResponseEntity<List<ReportDTO>> getAll() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/robot/{sn}")
    public ResponseEntity<List<ReportDTO>> bySn(@PathVariable String sn) {
        return ResponseEntity.ok(reportService.getReportsByRobotSn(sn));
    }
}