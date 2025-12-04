package com.codehows.taelimbe.pudureport.controller;

import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.service.ReportService;
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

    @PostMapping("/sync/store/time-range")
    public ResponseEntity<String> syncSingleStoreByTimeRange(
            @Valid @RequestBody StoreTimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncSingleStoreByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    @PostMapping("/sync/store/full-historical")
    public ResponseEntity<String> syncSingleStoreFullHistorical(
            @Valid @RequestBody StoreFullHistoricalSyncRequestDTO req
    ) {
        int count = reportService.syncSingleStoreFullHistorical(req.getStoreId());
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (과거 185일)");
    }

    @PostMapping("/sync/all-stores/time-range")
    public ResponseEntity<String> syncAllStoresByTimeRange(
            @Valid @RequestBody TimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncAllStoresByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 특정 기간)");
    }

    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {
        int count = reportService.syncAllStoresFullHistorical();
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 전체 기간)");
    }

    @GetMapping("/list/all")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/list/robot/{sn}")
    public ResponseEntity<List<ReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(reportService.getReportsByRobotSn(sn));
    }
}