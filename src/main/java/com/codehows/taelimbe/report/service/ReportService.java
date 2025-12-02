package com.codehows.taelimbe.report.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.report.dto.*;
import com.codehows.taelimbe.report.entity.Report;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.report.repository.ReportRepository;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PuduAPIClient puduAPIClient;
    private final ReportRepository reportRepository;
    private final RobotRepository robotRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    // ========== 1. 단일 매장 동기화 ==========

    /**
     * 특정 매장의 특정 기간 Report 동기화
     * 지정된 기간의 Report를 페이징하여 조회 및 저장
     * 한 번에 20개씩 조회하며, 데이터가 없을 때까지 자동으로 페이징 진행
     *
     * 사용 예시: 특정 매장의 어제 데이터만 재동기화
     *
     * @param req 매장 ID, 시작/종료 시간, 타임존 오프셋 포함
     * @return 저장된 Report 개수
     */
    @Transactional
    public int syncSingleStoreByTimeRange(StoreTimeRangeSyncRequestDTO req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();
        int totalCount = 0;
        int offset = req.getOffset();
        boolean hasMore = true;

        System.out.println("\n===== Sync Single Store by Time Range =====");
        System.out.println("Store ID: " + req.getStoreId());
        System.out.println("Time Range: " + req.getStartTime() + " ~ " + req.getEndTime());

        while (hasMore) {
            List<Map<String, String>> list = fetchReportList(
                    req.getStartTime(),
                    req.getEndTime(),
                    shopId,
                    req.getTimezoneOffset(),
                    offset
            );

            if (list.isEmpty()) {
                hasMore = false;
                break;
            }

            for (Map<String, String> item : list) {
                ReportDTO saved = saveReportDetailWithConversion(
                        item.get("sn"),
                        item.get("report_id"),
                        req.getStartTime(),
                        req.getEndTime(),
                        req.getTimezoneOffset(),
                        shopId
                );
                if (saved != null) totalCount++;
            }

            offset += 20;
        }

        System.out.println("Total Saved: " + totalCount);
        System.out.println("============================================\n");

        return totalCount;
    }

    /**
     * 특정 매장의 전체 기간(과거 185일) Report 동기화
     * 오늘 기준으로 과거 185일까지의 모든 Report를 조회하여 동기화
     * API 제한: 최대 180일까지만 조회 가능
     *
     * 사용 예시: 새 매장 초기 세팅 시
     *
     * @param storeId 매장 ID
     * @return 저장된 Report 개수
     */
    @Transactional
    public int syncSingleStoreFullHistorical(Long storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        long endTime = LocalDate.now().atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        long startTime = LocalDate.now().minusDays(185)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        System.out.println("\n===== Sync Single Store Full Historical (Last 185 Days) =====");
        System.out.println("Store ID: " + storeId);
        System.out.println("Start Date: " + LocalDate.now().minusDays(185));
        System.out.println("End Date: " + LocalDate.now());
        System.out.println("Start Timestamp: " + startTime);
        System.out.println("End Timestamp: " + endTime);

        int totalCount = 0;
        int offset = 0;
        boolean hasMore = true;
        int pageNum = 0;

        while (hasMore) {
            pageNum++;
            System.out.println("\n--- Page " + pageNum + " (offset: " + offset + ") ---");

            List<Map<String, String>> list = fetchReportList(
                    startTime,
                    endTime,
                    shopId,
                    0,
                    offset
            );

            System.out.println("Fetched: " + list.size() + " items");

            if (list.isEmpty()) {
                System.out.println("No more data. Stopping.");
                hasMore = false;
                break;
            }

            int pageCount = 0;
            for (Map<String, String> item : list) {
                ReportDTO saved = saveReportDetailWithConversion(
                        item.get("sn"),
                        item.get("report_id"),
                        startTime,
                        endTime,
                        0,
                        shopId
                );
                if (saved != null) {
                    totalCount++;
                    pageCount++;
                }
            }

            System.out.println("Saved in this page: " + pageCount);
            offset += 20;
        }

        System.out.println("\n===== Sync Complete =====");
        System.out.println("Total Saved: " + totalCount);
        System.out.println("==========================\n");

        return totalCount;
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
    @Transactional
    public int syncAllStoresByTimeRange(TimeRangeSyncRequestDTO req) {

        List<Store> stores = storeRepository.findAll();

        System.out.println("\n===== Sync All Stores by Time Range =====");
        System.out.println("Total Stores: " + stores.size());
        System.out.println("Start Time: " + req.getStartTime() + " (" +
                toLocal(req.getStartTime(), req.getTimezoneOffset()) + ")");
        System.out.println("End Time: " + req.getEndTime() + " (" +
                toLocal(req.getEndTime(), req.getTimezoneOffset()) + ")");

        int totalCount = 0;

        for (Store store : stores) {
            System.out.println("\n--- Processing Store: " + store.getStoreId() + " ---");

            try {
                StoreTimeRangeSyncRequestDTO syncReq = StoreTimeRangeSyncRequestDTO.builder()
                        .storeId(store.getStoreId())
                        .startTime(req.getStartTime())
                        .endTime(req.getEndTime())
                        .timezoneOffset(req.getTimezoneOffset())
                        .offset(0)
                        .build();

                int count = syncSingleStoreByTimeRange(syncReq);
                totalCount += count;

                System.out.println("Store " + store.getStoreId() + " Synced: " + count + " reports");

            } catch (Exception e) {
                System.out.println("Error syncing store " + store.getStoreId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n===== All Stores Time Range Sync Complete =====");
        System.out.println("Total Synced: " + totalCount);
        System.out.println("===============================================\n");

        return totalCount;
    }

    /**
     * 전체 매장의 전체 기간(과거 185일) Report 동기화
     * DB에 저장된 모든 매장을 순회하면서 각 매장의 과거 185일 Report를 조회하여 동기화
     *
     * ⚠️ 주의: 매장이 많을 경우 오래 걸릴 수 있음 (초기 세팅 또는 전체 마이그레이션용)
     *
     * 사용 예시: 시스템 초기 세팅 시 전체 데이터 동기화
     *
     * @return 저장된 전체 Report 개수
     */
    @Transactional
    public int syncAllStoresFullHistorical() {

        List<Store> stores = storeRepository.findAll();

        System.out.println("\n===== Sync All Stores Full Historical (Last 185 Days) =====");
        System.out.println("Total Stores: " + stores.size());

        int totalCount = 0;

        for (Store store : stores) {
            System.out.println("\n--- Processing Store: " + store.getStoreId() + " ---");

            try {
                int count = syncSingleStoreFullHistorical(store.getStoreId());
                totalCount += count;
            } catch (Exception e) {
                System.out.println("Error syncing store " + store.getStoreId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n===== All Stores Full Historical Sync Complete =====");
        System.out.println("Total Saved: " + totalCount);
        System.out.println("====================================================\n");

        return totalCount;
    }



    // ========== 3. Report 조회 ==========

    /**
     * 특정 기간의 Report를 DB에서 조회
     * @param startDate 시작 날짜 (yyyy-MM-dd 형식)
     * @param endDate 종료 날짜 (yyyy-MM-dd 형식)
     * @return 해당 기간의 Report 목록
     */
    public List<ReportDTO> getReport(String startDate, String endDate) {

        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusWeeks(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        return reportRepository.findByStartTimeBetween(startDateTime, endDateTime)
                .stream()
                .map(ReportDTO::createReportDTO)
                .toList();
    }

    /**
     * 모든 저장된 Report 조회
     * @return 전체 Report 목록
     */
    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Report ID로 특정 Report 조회
     *
     * @param id Report ID
     * @return Report 정보 DTO
     */
    public ReportDTO getReportById(Long id) {
        return toDto(reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found")));
    }

    /**
     * 로봇 시리얼 번호로 해당 로봇의 모든 Report 조회
     * @param sn 로봇 시리얼 번호
     * @return 해당 로봇의 Report 목록
     */
    public List<ReportDTO> getReportsByRobotSn(String sn) {
        return reportRepository.findByRobot_Sn(sn)
                .stream().map(this::toDto).toList();
    }

    // ========== Private Helper Methods ==========

    /**
     * Report 상세 정보를 Pudu API에서 조회하여 DB에 저장
     * taskId 중복 체크하여 중복이면 저장하지 않음
     *
     * @param sn 로봇 시리얼 번호
     * @param reportId Report ID (taskId로 사용)
     * @param start 시작 시간
     * @param end 종료 시간
     * @param timezoneOffset 타임존 오프셋
     * @param shopId 샵 ID
     * @return 저장된 Report 정보 DTO
     */
    private ReportDTO saveReportDetailWithConversion(
            String sn, String reportId, long start, long end, int timezoneOffset, Long shopId
    ) {
        JsonNode detail = fetchReportDetail(sn, reportId, start, end, timezoneOffset, shopId);
        if (detail == null) {
            return null;
        }

        Long taskId = Long.parseLong(reportId);
        Optional<Report> existing = reportRepository.findByTaskId(taskId);

        if (existing.isPresent()) {
            System.out.println("Report with taskId " + taskId + " already exists. Skipping.");
            return null;
        }

        Robot robot = robotRepository.findBySn(sn)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));

        String mapName = null;
        String mapUrl = null;

        try {
            JsonNode floorListNode = detail.path("floor_list");
            JsonNode floorList = floorListNode.isTextual()
                    ? mapper.readTree(floorListNode.asText())
                    : floorListNode;

            if (floorList.isArray() && floorList.size() > 0) {
                JsonNode first = floorList.get(0);
                mapName = first.path("map_name").asText(null);
                mapUrl = first.path("task_result_url").asText(
                        first.path("task_local_url").asText(null)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Report report = Report.builder()
                .taskId(taskId)
                .status(detail.path("status").asInt())
                .startTime(toLocal(detail.path("start_time").asLong(), timezoneOffset))
                .endTime(toLocal(detail.path("end_time").asLong(), timezoneOffset))
                .cleanTime(detail.path("clean_time").floatValue())
                .taskArea(detail.path("task_area").floatValue())
                .cleanArea(detail.path("clean_area").floatValue())
                .mode(detail.path("mode").asInt())
                .costBattery(detail.path("cost_battery").asLong())
                .costWater(detail.path("cost_water").asLong())
                .mapName(mapName)
                .mapUrl(mapUrl)
                .robot(robot)
                .build();

        return toDto(reportRepository.save(report));
    }

    /**
     * Pudu API에서 Report 목록을 페이징하여 조회
     * @param start 시작 시간 (Unix timestamp)
     * @param end 종료 시간 (Unix timestamp)
     * @param shopId 샵 ID
     * @param timezoneOffset 타임존 오프셋
     * @param offset 페이징 offset
     * @return 로봇 SN과 Report ID 맵 리스트
     */
    private List<Map<String, String>> fetchReportList(
            long start, long end, Long shopId, int timezoneOffset, int offset
    ) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time", start)
                    .queryParam("end_time", end)
                    .queryParam("shop_id", shopId)
                    .queryParam("offset", offset)
                    .queryParam("limit", 20)
                    .queryParam("timezone_offset", timezoneOffset)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");
            JsonNode list = mapper.readTree(res.getBody()).path("data").path("list");

            if (list.isArray()) {
                for (JsonNode n : list) {
                    Map<String, String> map = new HashMap<>();
                    map.put("sn", n.path("sn").asText());
                    map.put("report_id", n.path("report_id").asText());
                    result.add(map);
                }
            }

        } catch (Exception e) {
            System.out.println("Exception in fetchReportList: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Pudu API에서 특정 Report의 상세 정보 조회
     * @param sn 로봇 시리얼 번호
     * @param reportId Report ID
     * @param start 시작 시간
     * @param end 종료 시간
     * @param timezoneOffset 타임존 오프셋
     * @param shopId 샵 ID
     * @return Report 상세 정보 JSON 노드
     */
    private JsonNode fetchReportDetail(
            String sn, String reportId, long start, long end, int timezoneOffset, Long shopId
    ) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn", sn)
                    .queryParam("report_id", reportId)
                    .queryParam("start_time", start)
                    .queryParam("end_time", end)
                    .queryParam("timezone_offset", timezoneOffset)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            return mapper.readTree(res.getBody()).path("data");

        } catch (Exception ignored) {}

        return null;
    }

    /**
     * Report 엔티티를 DTO로 변환
     * @param r Report 엔티티
     * @return 변환된 Report DTO
     */
    private ReportDTO toDto(Report r) {
        return ReportDTO.builder()
                .reportId(r.getReportId())
                .taskId(r.getTaskId())
                .status(r.getStatus())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .cleanTime(r.getCleanTime())
                .taskArea(r.getTaskArea())
                .cleanArea(r.getCleanArea())
                .mode(r.getMode())
                .costBattery(r.getCostBattery())
                .costWater(r.getCostWater())
                .mapName(r.getMapName())
                .mapUrl(r.getMapUrl())
                .robotId(r.getRobot().getRobotId())
                .build();
    }

    /**
     * Unix timestamp를 LocalDateTime으로 변환
     */
    private LocalDateTime toLocal(long epoch, int timezoneOffset) {
        long adjusted = epoch + (timezoneOffset * 60L);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(adjusted), ZoneId.systemDefault());
    }
}
