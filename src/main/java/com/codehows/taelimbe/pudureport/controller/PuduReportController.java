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
@RequestMapping("/api/report")
public class PuduReportController {

    private final PuduReportService puduReportService;

    @PostMapping("/sync/store/time-range")
    public ResponseEntity<String> syncSingleStoreByTimeRange(
            @Valid @RequestBody StoreTimeRangeSyncRequestDTO req
    ) {
        int count = puduReportService.syncSingleStoreByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }


    @PostMapping("/sync/all-stores/time-range")
    public ResponseEntity<String> syncAllStoresByTimeRange(
            @Valid @RequestBody TimeRangeSyncRequestDTO req
    ) {
        int count = puduReportService.syncAllStoresByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 특정 기간)");
    }

    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {
        int count = puduReportService.syncAllStoresFullHistorical();
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 전체 기간)");
    }

    @GetMapping("/list/all")
    public ResponseEntity<List<PuduReportDTO>> getAllReports() {
        return ResponseEntity.ok(puduReportService.getAllReports());
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<PuduReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(puduReportService.getReportById(id));
    }

    @GetMapping("/list/robot/{sn}")
    public ResponseEntity<List<PuduReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(puduReportService.getReportsByRobotSn(sn));
    }

    @GetMapping("/list")
    public ResponseEntity<List<PuduReportDTO>> getReports(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end
    ){
        return ResponseEntity.ok(puduReportService.getReport(start, end));
    }

}