package com.codehows.taelimbe.report.controller;

import com.codehows.taelimbe.report.dto.*;
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

    // ========== 1. 단일 매장 동기화 ==========

    /**
     * 특정 매장의 특정 기간 Report 동기화
     * 요청한 매장의 지정된 기간 Report를 Pudu API에서 조회하여 DB에 저장
     * 페이징 처리로 한 번에 20개씩 자동 조회
     *
     * 사용 예시: 특정 매장의 어제 데이터만 재동기화
     *
     * @param req 매장 ID, 시작/종료 시간, 타임존 오프셋 포함
     * @return 저장된 Report 개수
     */
    @PostMapping("/sync/store/time-range")
    public ResponseEntity<String> syncSingleStoreByTimeRange(
            @Valid @RequestBody StoreTimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncSingleStoreByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    /**
     * 특정 매장의 전체 기간(185일) Report 동기화
     * 오늘 기준으로 과거 185일까지의 모든 Report를 조회하여 동기화
     * API 제한: 최대 180일까지만 조회 가능
     *
     * 사용 예시: 새 매장 초기 세팅 시
     *
     * @param storeId 매장 ID
     * @return 저장된 Report 개수
     */
    @PostMapping("/sync/store/full-historical")
    public ResponseEntity<String> syncSingleStoreFullHistorical(
            @RequestParam Long storeId
    ) {
        int count = reportService.syncSingleStoreFullHistorical(storeId);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (과거 185일)");
    }

    // ========== 2. 전체 매장 동기화 ==========

    /**
     * 전체 매장의 특정 기간 Report 동기화
     * 모든 매장의 지정된 기간 Report를 Pudu API에서 조회하여 DB에 저장
     *
     * 사용 예시:
     * - 어제 하루치 데이터만 전체 매장 재동기화
     * - 지난 주 데이터만 전체 매장 재동기화
     * - 장애 복구 후 특정 시간대 데이터만 재동기화
     *
     * @param req 시작/종료 시간, 타임존 오프셋 포함 (storeId 없음)
     * @return 저장된 전체 Report 개수
     */
    @PostMapping("/sync/all-stores/time-range")
    public ResponseEntity<String> syncAllStoresByTimeRange(
            @Valid @RequestBody TimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncAllStoresByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 특정 기간)");
    }

    /**
     * 전체 매장의 전체 기간(185일) Report 동기화
     * DB에 저장된 모든 매장을 순회하면서 각 매장의 과거 185일 Report를 조회하여 동기화
     *
     * ⚠️ 주의: 매장이 많을 경우 오래 걸릴 수 있음 (초기 세팅 또는 전체 마이그레이션용)
     *
     * 사용 예시: 시스템 초기 세팅 시 전체 데이터 동기화
     *
     * @return 저장된 전체 Report 개수
     */
    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {
        int count = reportService.syncAllStoresFullHistorical();
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 전체 기간)");
    }

    // ========== 3. Report 상세 저장 ==========

    /**
     * Report 상세 정보 저장
     * 특정 Report의 상세 정보를 Pudu API에서 조회하여 DB에 저장
     *
     * @param req 매장 ID, 로봇 SN, Report ID, 시작/종료 시간 포함
     * @return 저장된 Report 정보 DTO
     */
    @PostMapping("/detail/save")
    public ResponseEntity<ReportDTO> saveReportDetail(
            @Valid @RequestBody ReportDetailRequestDTO req
    ) {
        return ResponseEntity.ok(reportService.saveReportDetail(req));
    }

    // ========== 4. Report 조회 ==========

    /**
     * 모든 Report 조회
     * DB에 저장된 모든 Report를 조회
     *
     * @return 전체 Report 목록
     */
    @GetMapping("/list/all")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    /**
     * Report ID로 특정 Report 조회
     *
     * @param id Report ID
     * @return Report 정보 DTO
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    /**
     * 로봇별 Report 조회
     * 특정 로봇(시리얼 번호)의 모든 Report를 조회
     *
     * @param sn 로봇 시리얼 번호
     * @return 해당 로봇의 Report 목록
     */
    @GetMapping("/list/robot/{sn}")
    public ResponseEntity<List<ReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(reportService.getReportsByRobotSn(sn));
    }
}