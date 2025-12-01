package com.codehows.taelimbe.report.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.report.dto.ReportSyncRequestDTO;
import com.codehows.taelimbe.report.dto.ReportDetailRequestDTO;
import com.codehows.taelimbe.report.dto.ReportDTO;
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


1
    @Transactional
    public int syncReports(ReportSyncRequestDTO req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();
        int totalCount = 0;

        // offset을 0부터 시작해서 계속 증가시키며 모든 데이터 가져오기
        int offset = req.getOffset();
        boolean hasMore = true;

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
                ReportDTO saved = saveReportDetailByParams(
                        item.get("sn"),
                        item.get("report_id"),
                        req.getStartTime(),
                        req.getEndTime(),
                        req.getTimezoneOffset(),
                        shopId
                );
                if (saved != null) totalCount++;
            }

            offset += 20;  // 다음 페이지
        }

        return totalCount;
    }

    // ReportService에 추가할 메서드

    /**
     * 모든 매장의 과거 185일 데이터를 동기화
     */
    @Transactional
    public int syncAllStoresHistoricalReports() {

        // DB에서 모든 Store 조회
        List<Store> stores = storeRepository.findAll();

        System.out.println("\n===== Sync All Stores Historical Data =====");
        System.out.println("Total Stores: " + stores.size());

        int totalCount = 0;

        for (Store store : stores) {
            System.out.println("\n--- Processing Store: " + store.getStoreId() + " ---");

            try {
                int count = syncAllHistoricalReports(store.getStoreId());
                totalCount += count;
            } catch (Exception e) {
                System.out.println("Error syncing store " + store.getStoreId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("\n===== All Stores Sync Complete =====");
        System.out.println("Total Saved: " + totalCount);
        System.out.println("=====================================\n");

        return totalCount;
    }

    /**
     * 단일 매장의 과거 185일 데이터를 동기화
     */
    @Transactional
    public int syncAllHistoricalReports(Long storeId) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        // 오늘 자정 기준
        long endTime = LocalDate.now().atTime(LocalTime.MAX)
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        // 185일 전
        long startTime = LocalDate.now().minusDays(185)
                .atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toEpochSecond();

        System.out.println("Store ID: " + storeId);
        System.out.println("Start Date: " + LocalDate.now().minusDays(185));
        System.out.println("End Date: " + LocalDate.now());

        int totalCount = 0;
        int offset = 0;
        boolean hasMore = true;
        int pageNum = 0;

        while (hasMore) {
            pageNum++;
            System.out.println("  Page " + pageNum + " (offset: " + offset + ")");

            List<Map<String, String>> list = fetchReportList(
                    startTime,
                    endTime,
                    shopId,
                    0,
                    offset
            );

            if (list.isEmpty()) {
                hasMore = false;
                break;
            }

            int pageCount = 0;
            for (Map<String, String> item : list) {
                ReportDTO saved = saveReportDetailByParams(
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

            System.out.println("  Saved: " + pageCount);
            offset += 20;
        }

        System.out.println("Store " + storeId + " Total Saved: " + totalCount);
        return totalCount;
    }

    @Transactional
    public ReportDTO saveDetail(ReportDetailRequestDTO req) {

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        return saveReportDetailByParams(
                req.getSn(),
                req.getReportId(),
                req.getStartTime(),
                req.getEndTime(),
                req.getTimezoneOffset(),
                store.getShopId()
        );
    }

    private ReportDTO saveReportDetailByParams(
            String sn,
            String reportId,
            long start,
            long end,
            int timezoneOffset,
            Long shopId
    ) {
        JsonNode detail = fetchReportDetail(sn, reportId, start, end, timezoneOffset, shopId);

        if (detail == null) return null;

        return convertAndSave(detail, sn, reportId, timezoneOffset);
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::toDto).toList();
    }

    public ReportDTO getReportById(Long id) {
        return toDto(reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found")));
    }

    public List<ReportDTO> getReportsByRobotSn(String sn) {
        return reportRepository.findByRobot_Sn(sn)
                .stream().map(this::toDto).toList();
    }

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
                    .queryParam("offset", offset)  // 동적 offset
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

    private ReportDTO convertAndSave(JsonNode n, String sn, String reportId, int timezoneOffset) {

        // taskId 중복 체크
        Long taskId = Long.parseLong(reportId);
        Optional<Report> existing = reportRepository.findByTaskId(taskId);

        if (existing.isPresent()) {
            System.out.println("Report with taskId " + taskId + " already exists. Skipping.");
            return null;  // 중복이면 null 반환 (저장 안 함)
        }

        Robot robot = robotRepository.findBySn(sn)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));

        JsonNode floorListNode = n.path("floor_list");
        String mapName = null;
        String mapUrl = null;

        try {
            if (floorListNode.isTextual()) {
                String floorListJson = floorListNode.asText();
                JsonNode floorList = mapper.readTree(floorListJson);

                if (floorList.isArray() && floorList.size() > 0) {
                    JsonNode first = floorList.get(0);
                    mapName = first.path("map_name").asText(null);
                    mapUrl = first.path("task_result_url").asText(
                            first.path("task_local_url").asText(null)
                    );
                }
            } else if (floorListNode.isArray() && floorListNode.size() > 0) {
                JsonNode first = floorListNode.get(0);
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
                .status(n.path("status").asInt())
                .startTime(toLocal(n.path("start_time").asLong(), timezoneOffset))
                .endTime(toLocal(n.path("end_time").asLong(), timezoneOffset))
                .cleanTime(n.path("clean_time").floatValue())
                .taskArea(n.path("task_area").floatValue())
                .cleanArea(n.path("clean_area").floatValue())
                .mode(n.path("mode").asInt())
                .costBattery(n.path("cost_battery").asLong())
                .costWater(n.path("cost_water").asLong())
                .mapName(mapName)
                .mapUrl(mapUrl)
                .robot(robot)
                .build();

        return toDto(reportRepository.save(report));
    }
    private ReportDTO toDto(Report r) {
        return ReportDTO.builder()
                .reportId(r.getReportId())
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

    private LocalDateTime toLocal(long epoch, int timezoneOffset) {
        long adjusted = epoch + (timezoneOffset * 60L);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(adjusted), ZoneId.systemDefault());
    }
}