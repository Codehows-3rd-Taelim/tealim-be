package com.codehows.taelimbe.report.service;

import com.codehows.taelimbe.client.PuduAPIClient;
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

        // 문자열 형태의 시작일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 시작 시간(00:00:00)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        // 문자열 형태의 종료일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 마지막 시간(23:59:59.999999999)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        return reportRepository.findByStartTimeBetween(startDateTime, endDateTime)
                .stream()
                .map(ReportDTO::createReportDTO)
                .toList();
    }

    // 1) 매장 단위 전체 동기화
    @Transactional
    public int syncReportsByStoreId(
            Long storeId,
            long queryStartTime,
            long queryEndTime,
            int timezoneOffset
    ) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        // offset=0, limit=20 자동 적용 (고정)
        List<Map<String, String>> list =
                fetchReportList(queryStartTime, queryEndTime, shopId, timezoneOffset);

        int count = 0;
        for (Map<String, String> item : list) {
            ReportDTO saved = saveReportDetail(
                    item.get("sn"),
                    item.get("report_id"),
                    queryStartTime,
                    queryEndTime,
                    timezoneOffset,
                    shopId
            );
            if (saved != null) count++;
        }
        return count;
    }

    // 2) 단일 저장 (Store 기준)
    @Transactional
    public ReportDTO saveReportDetailByStoreId(
            Long storeId,
            String sn,
            String reportId,
            long queryStartTime,
            long queryEndTime,
            int timezoneOffset
    ) {

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        return saveReportDetail(
                sn,
                reportId,
                queryStartTime,
                queryEndTime,
                timezoneOffset,
                store.getShopId()
        );
    }

    // 3) 단일 저장 (내부)
    @Transactional
    public ReportDTO saveReportDetail(
            String sn,
            String reportId,
            long queryStartTime,
            long queryEndTime,
            int timezoneOffset,
            Long shopId
    ) {
        JsonNode detail = fetchReportDetail(
                sn,
                reportId,
                queryStartTime,
                queryEndTime,
                timezoneOffset,
                shopId
        );

        if (detail == null) return null;

        return convertAndSave(detail, sn, timezoneOffset);
    }

    // 4) 전체 조회
    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll()
                .stream().map(this::toDto).toList();
    }

    // 5) report id 조회
    public ReportDTO getReportById(Long id) {
        return toDto(reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found")));
    }

    // 5) sn 조회
    public List<ReportDTO> getReportsByRobotSn(String sn) {
        return reportRepository.findByRobot_Sn(sn)
                .stream().map(this::toDto).toList();
    }

    // API 1) Cleaning Report - List 조회 (offset/limit 고정)
    private List<Map<String, String>> fetchReportList(
            long queryStartTime,
            long queryEndTime,
            Long shopId,
            int timezoneOffset
    ) {
        List<Map<String, String>> result = new ArrayList<>();

        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time", queryStartTime)
                    .queryParam("end_time", queryEndTime)
                    .queryParam("shop_id", shopId)
                    .queryParam("offset", 0)
                    .queryParam("limit", 20)

                    .queryParam("timezone_offset", timezoneOffset)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            JsonNode list = mapper.readTree(res.getBody())
                    .path("data")
                    .path("list");

            if (list.isArray()) {
                for (JsonNode n : list) {
                    Map<String, String> map = new HashMap<>();
                    map.put("sn", n.path("sn").asText());
                    map.put("report_id", n.path("report_id").asText());
                    result.add(map);
                }
            }

        } catch (Exception ignored) {}

        return result;
    }

    // API 2) Cleaning Report - Detail 조회
    private JsonNode fetchReportDetail(
            String sn,
            String reportId,
            long queryStartTime,
            long queryEndTime,
            int timezoneOffset,
            Long shopId
    ) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn", sn)
                    .queryParam("report_id", reportId)
                    .queryParam("start_time", queryStartTime)
                    .queryParam("end_time", queryEndTime)
                    .queryParam("timezone_offset", timezoneOffset)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            ResponseEntity<String> res = puduAPIClient.callPuduAPI(url, "GET");

            System.out.println("DETAIL URL = " + url);
            System.out.println("DETAIL RESPONSE = " + res.getBody());

            return mapper.readTree(res.getBody()).path("data");

        } catch (Exception ignored) {}

        return null;
    }

    // 변환 + DB 저장
    private ReportDTO convertAndSave(JsonNode n, String sn, int timezoneOffset) {

        Robot robot = robotRepository.findBySn(sn)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));

        Report report = Report.builder()
                .status(n.path("status").asInt())
                .startTime(toLocal(n.path("start_time").asLong(), timezoneOffset))
                .endTime(toLocal(n.path("end_time").asLong(), timezoneOffset))
                .cleanTime(n.path("clean_time").floatValue())
                .taskArea(n.path("task_area").floatValue())
                .cleanArea(n.path("clean_area").floatValue())
                .mode(n.path("mode").asInt())
                .costBattery(n.path("cost_battery").asLong())
                .costWater(n.path("cost_water").asLong())
                .mapName(n.path("map_name").asText(null))
                .mapUrl(n.path("map_url").asText(null))
                .robot(robot)
                .build();

        return toDto(reportRepository.save(report));
    }


    // ================================================
    // DTO 변환
    // ================================================
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
                .robotSn(r.getRobot().getSn())
                .build();
    }


    // ================================================
    // Timestamp 변환 함수
    // ================================================
    private LocalDateTime toLocal(long epoch, int timezoneOffset) {
        long adjusted = epoch + (timezoneOffset * 60L);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(adjusted), ZoneId.systemDefault());
    }
}
