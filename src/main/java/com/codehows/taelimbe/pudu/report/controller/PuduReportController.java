package com.codehows.taelimbe.pudu.report.controller;

import com.codehows.taelimbe.pudu.report.dto.*;
import com.codehows.taelimbe.pudu.report.service.PuduReportFullHistoricalFacade;
import com.codehows.taelimbe.pudu.report.service.PuduReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/report")
public class PuduReportController {

    private final PuduReportService puduReportService;
    private final PuduReportFullHistoricalFacade puduReportFullHistoricalFacade;

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

    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {

        puduReportFullHistoricalFacade.syncAllStores6MonthAsync();

        return ResponseEntity.accepted().body(
                "전체 매장 6개월 보고서 동기화 작업을 시작했습니다. " +
                        "작업은 백그라운드에서 계속 진행되며, 완료까지 8분 정도 시간이 걸릴 수 있습니다."
        );
    }


    // id로 보고서 가져오기
    @GetMapping("/detail/{id}")
    public ResponseEntity<PuduReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(puduReportService.getReportById(id));
    }


    // 특이사항 입력
    @PutMapping("/{puduReportId}/remark")
    public ResponseEntity<PuduReportDTO> updateRemark(
            @PathVariable Long puduReportId,
            @RequestBody RemarkUpdateRequestDTO req
    ) {
        PuduReportDTO dto = puduReportService.updateRemark(
                puduReportId,
                req.getRemark()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    public ResponseEntity<Page<PuduReportResponseDTO>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long filterStoreId,
            @RequestParam(required = false) String sn,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "startTime") String sortKey,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        LocalDateTime s;
        LocalDateTime e;

        try {
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            s = LocalDateTime.parse(startDate, formatter);
            e = LocalDateTime.parse(endDate, formatter);
        } catch (Exception ex) {
            s = LocalDate.parse(startDate).atStartOfDay();
            e = LocalDate.parse(endDate).atTime(LocalTime.MAX);
        }

        return ResponseEntity.ok(
                puduReportService.getReportsPage(
                        storeId,
                        filterStoreId,
                        sn,
                        s,
                        e,
                        page,
                        size,
                        sortKey,
                        sortOrder
                )
        );
    }

    // 해당 store의 report 조회
    @GetMapping("/list")
    public ResponseEntity<List<PuduReportResponseDTO>> getReportsforDashboard(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        List<PuduReportResponseDTO> reports;

        if (storeId == null) {
            // 전체 매장
            reports = puduReportService.getReportsAllStores(startDate, endDate);
        } else {
            // 특정 매장
            reports = puduReportService.getReportByStore(storeId, startDate, endDate);
        }

        return ResponseEntity.ok(reports);
    }
}

