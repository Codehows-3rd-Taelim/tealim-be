package com.codehows.taelimbe.pudureport.controller;

import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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




    // id로 보고서 가져오기
    @GetMapping("/detail/{id}")
    public ResponseEntity<PuduReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(puduReportService.getReportById(id));
    }



    @GetMapping
    public ResponseEntity<?> getReports(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long filterStoreId,
            @RequestParam(required = false) String sn,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "startTime") String sortKey,
            @RequestParam(defaultValue = "desc") String sortOrder
    ) {
        LocalDateTime s = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime e = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        // 페이징 O
        if (page != null && size != null) {
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

        // 페이징 X
        return ResponseEntity.ok(
                puduReportService.getReports(
                        storeId,
                        filterStoreId,
                        sn,
                        s,
                        e,
                        sortKey,
                        sortOrder
                )
        );
    }
}
