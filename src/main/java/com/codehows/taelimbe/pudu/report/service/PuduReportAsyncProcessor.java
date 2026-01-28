  package com.codehows.taelimbe.pudu.report.service;

import com.codehows.taelimbe.pudu.report.entity.PuduReport;
import com.codehows.taelimbe.pudu.report.repository.PuduReportRepository;
import com.codehows.taelimbe.pudu.PuduAPIClient;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PuduReportAsyncProcessor {

    private final PuduAPIClient puduAPIClient;
    private final PuduReportRepository puduReportRepository;
    private final RobotRepository robotRepository;
    private final ObjectMapper mapper = new ObjectMapper();


    // 청소 보고서 리스트 조회
    public List<JsonNode> fetchList(LocalDateTime s, LocalDateTime e,
                                    Long shopId, int tz, int offset) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time", epoch(s))
                    .queryParam("end_time", epoch(e))
                    .queryParam("shop_id", shopId)
                    .queryParam("offset", offset)
                    .queryParam("limit", 20)
                    .queryParam("timezone_offset", tz)
                    .toUriString();

            JsonNode listNode = mapper.readTree(
                    puduAPIClient.callPuduAPI(url, "GET").getBody()
            ).path("data").path("list");

            List<JsonNode> list = new ArrayList<>();
            if (listNode.isArray()) {
                listNode.forEach(list::add);
            }
            return list;

        } catch (Exception ex) {
            return List.of();
        }
    }


    @Async("PuduReportSyncExecutor")
    public CompletableFuture<PuduReport> convertAsync(
            String sn, String reportIdStr,
            LocalDateTime start, LocalDateTime end,
            int tz, Long shopId) {

        try {
            Long reportId = safeLong(reportIdStr);
            if (reportId == null || puduReportRepository.findByReportId(reportId).isPresent())
                return done();

            Robot robot = robotRepository.findBySn(sn).orElse(null);
            if (robot == null) return done();

            JsonNode data = fetchDetail(sn, reportIdStr, start, end, tz, shopId);
            if (data == null) return done();

            Map<String, String> floor = parseFloor(data);

            return CompletableFuture.completedFuture(
                    PuduReport.builder()
                            .reportId(reportId)
                            .status(data.path("status").asInt())
                            .startTime(epochToLocalDateTime(data.path("start_time").asLong()))
                            .endTime(epochToLocalDateTime(data.path("end_time").asLong()))
                            .cleanTime(data.path("clean_time").asDouble() > 0 ? data.path("clean_time").floatValue() : null)
                            .taskArea(data.path("task_area").asDouble() > 0 ? data.path("task_area").floatValue() : null)
                            .cleanArea(data.path("clean_area").asDouble() > 0 ? data.path("clean_area").floatValue() : null)
                            .mode(data.path("mode").asInt())
                            .costBattery(data.path("cost_battery").asLong())
                            .costWater(data.path("cost_water").asLong())
                            .mapName(floor.get("mapName"))
                            .mapUrl(floor.get("mapUrl"))
                            .robot(robot)
                            .build()
            );

        } catch (Exception e) {
            e.printStackTrace();
            return done();
        }
    }


    // 상세 보고서 조회
    private JsonNode fetchDetail(
            String sn, String id, LocalDateTime s, LocalDateTime e, int tz, Long shopId) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn", sn)
                    .queryParam("report_id", id)
                    .queryParam("start_time", epoch(s))
                    .queryParam("end_time", epoch(e))
                    .queryParam("timezone_offset", tz)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            return mapper.readTree(
                    puduAPIClient.callPuduAPI(url, "GET").getBody()
            ).path("data");

        } catch (Exception ex) {
            return null;
        }
    }


    public PuduReport convertSyncOnly(
            String sn, String reportIdStr,
            LocalDateTime start, LocalDateTime end,
            int tz, Long shopId
    ) {
        try {
            Long reportId = safeLong(reportIdStr);
            if (reportId == null || puduReportRepository.findByReportId(reportId).isPresent())
                return null;

            Robot robot = robotRepository.findBySn(sn).orElse(null);
            if (robot == null) return null;

            JsonNode data = fetchDetail(sn, reportIdStr, start, end, tz, shopId);
            if (data == null) return null;

            Map<String, String> floor = parseFloor(data);

            return PuduReport.builder()
                    .reportId(reportId)
                    .status(data.path("status").asInt())
                    .startTime(epochToLocalDateTime(data.path("start_time").asLong()))
                    .endTime(epochToLocalDateTime(data.path("end_time").asLong()))
                    .cleanTime(data.path("clean_time").asDouble() > 0
                            ? data.path("clean_time").floatValue() : null)
                    .taskArea(data.path("task_area").asDouble() > 0
                            ? data.path("task_area").floatValue() : null)
                    .cleanArea(data.path("clean_area").asDouble() > 0
                            ? data.path("clean_area").floatValue() : null)
                    .mode(data.path("mode").asInt())
                    .costBattery(data.path("cost_battery").asLong())
                    .costWater(data.path("cost_water").asLong())
                    .mapName(floor.get("mapName"))
                    .mapUrl(floor.get("mapUrl"))
                    .robot(robot)
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // floor_list JSON 파싱
    private Map<String, String> parseFloor(JsonNode data) {
        Map<String, String> floor = new HashMap<>();

        try {
            JsonNode floorList = data.path("floor_list");

            // floor_list가 배열인 경우
            if (floorList.isArray() && !floorList.isEmpty()) {
                JsonNode f = floorList.get(0);
                floor.put("mapName", f.path("map_name").asText());

                // task_result_url이 있으면 우선, 없으면 task_local_url
                String mapUrl = f.path("task_result_url").asText();
                if (mapUrl == null || mapUrl.isEmpty()) {
                    mapUrl = f.path("task_local_url").asText();
                }
                floor.put("mapUrl", mapUrl);
            }
            // floor_list가 JSON 문자열인 경우
            else if (floorList.isTextual()) {
                String floorStr = floorList.asText();
                JsonNode parsed = mapper.readTree(floorStr);

                if (parsed.isArray() && !parsed.isEmpty()) {
                    JsonNode f = parsed.get(0);
                    floor.put("mapName", f.path("map_name").asText());

                    String mapUrl = f.path("task_result_url").asText();
                    if (mapUrl == null || mapUrl.isEmpty()) {
                        mapUrl = f.path("task_local_url").asText();
                    }
                    floor.put("mapUrl", mapUrl);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 실패 시 빈 맵 반환
        }

        return floor.isEmpty() ? Map.of("mapName", "", "mapUrl", "") : floor;
    }


    private static CompletableFuture<PuduReport> done() {
        return CompletableFuture.completedFuture(null);
    }


    private static LocalDateTime epochToLocalDateTime(long epoch) {
        if (epoch <= 0) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(epoch),
                ZoneId.systemDefault()
        );
    }


    private static Long epoch(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toEpochSecond();
    }


    private static Long safeLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}