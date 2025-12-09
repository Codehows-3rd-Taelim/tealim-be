package com.codehows.taelimbe.pudureport.controller;

import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
public class PuduReportController {

    private final PuduReportService puduReportService;

    // 단일 매장 특정 기간 보고서 조회
    @PostMapping("/sync/store/time-range")
    public ResponseEntity<String> syncSingleStoreByTimeRange(
            @Valid @RequestBody StoreTimeRangeSyncRequestDTO req
    ) {
        int count = puduReportService.syncSingleStoreByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    // 전체 매장 특정 기간 보고서 조회
    @PostMapping("/sync/all-stores/time-range")
    public ResponseEntity<String> syncAllStoresByTimeRange(
            @Valid @RequestBody TimeRangeSyncRequestDTO req
    ) {
        int count = puduReportService.syncAllStoresByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 특정 기간)");
    }

    // 전체 매장 최대기간(6개월) 보고서 조회
    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {
        int count = puduReportService.syncAllStoresFullHistorical();
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 전체 기간)");
    }

    // db에서 상세 보고서 목록 가져오기
    @GetMapping("/list/all")
    public ResponseEntity<List<PuduReportDTO>> getAllReports() {
        return ResponseEntity.ok(puduReportService.getAllReports());
    }

    // 해당 store의 report 조회
    @GetMapping("/list")
    public ResponseEntity<List<PuduReportResponseDTO>> getReports(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {

        List<PuduReportResponseDTO> reports = puduReportService.getReports(storeId, startDate, endDate);
        return ResponseEntity.ok(reports);
    }

    // id로 보고서 가져오기
    @GetMapping("/detail/{id}")
    public ResponseEntity<PuduReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(puduReportService.getReportById(id));
    }

    // 로봇별 Report 조회
    @GetMapping("/list/robot/{sn}")
    public ResponseEntity<List<PuduReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(puduReportService.getReportsByRobotSn(sn));
    }
}
