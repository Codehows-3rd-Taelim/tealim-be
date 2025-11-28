package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.ReportDTO;
import com.codehows.taelimbe.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    // ================= 1) 전체 Report 동기화 =================
    @PostMapping("/sync")
    public ResponseEntity<String> syncReports(
            @RequestParam Long storeId,
            @RequestParam(name = "start_time") long queryStartTime,
            @RequestParam(name = "end_time") long queryEndTime,
            @RequestParam(defaultValue = "0") int timezoneOffset
    ) {
        int count = reportService.syncReportsByStoreId(
                storeId,
                queryStartTime,
                queryEndTime,
                timezoneOffset
        );
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    // ================= 2) 단일 Report 상세 저장 =================
    @PostMapping("/save-detail")
    public ResponseEntity<ReportDTO> saveReportDetail(
            @RequestParam Long storeId,
            @RequestParam String sn,
            @RequestParam String report_id,
            @RequestParam(name = "start_time") long queryStartTime,
            @RequestParam(name = "end_time") long queryEndTime,
            @RequestParam(defaultValue = "0") int timezoneOffset
    ) {
        return ResponseEntity.ok(
                reportService.saveReportDetailByStoreId(
                        storeId,
                        sn,
                        report_id,
                        queryStartTime,
                        queryEndTime,
                        timezoneOffset
                )
        );
    }

    // ================= 3) 전체 조회 =================
    @GetMapping("/list")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    // ================= 4) ID 조회 =================
    @GetMapping("/{reportId}")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long reportId) {
        return ResponseEntity.ok(reportService.getReportById(reportId));
    }

    // ================= 5) 로봇 SN 기준 조회 =================
    @GetMapping("/robot/{sn}")
    public ResponseEntity<List<ReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(reportService.getReportsByRobotSn(sn));
    }


}
