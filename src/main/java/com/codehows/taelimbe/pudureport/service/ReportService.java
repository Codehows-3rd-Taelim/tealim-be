package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.entity.Report;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.pudureport.repository.ReportRepository;
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

    private ReportDTO saveReportDetailWithConversion(
            String sn, String reportIdStr, long start, long end, int timezoneOffset, Long shopId
    ) {
        JsonNode detail = fetchReportDetail(sn, reportIdStr, start, end, timezoneOffset, shopId);
        if (detail == null) {
            return null;
        }

        Long reportId = Long.parseLong(reportIdStr);
        Optional<Report> existing = reportRepository.findByReportId(reportId);

        if (existing.isPresent()) {
            System.out.println("Report with reportId " + reportId + " already exists. Skipping.");
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
                .reportId(reportId)
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

    private ReportDTO toDto(Report r) {
        return ReportDTO.builder()
                .puduReportId(r.getPuduReportId())
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
