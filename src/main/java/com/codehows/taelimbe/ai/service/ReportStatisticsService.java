package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.*;
import com.codehows.taelimbe.ai.dto.ReportStatistics.*;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportStatisticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<Integer, String> STATUS_LABEL_MAP = Map.of(
            0, "기타",
            1, "기타",
            2, "일시정지",
            3, "중단",
            4, "정상 완료",
            5, "예외",
            6, "취소"
    );

    private final PuduReportRepository puduReportRepository;

    /**
     * DB 집계 쿼리 기반 통계 계산 (신규 방식)
     */
    public ReportStatistics compute(LocalDateTime start, LocalDateTime end, Long storeId) {
        // 전체 요약
        OverallSummaryDTO summary = storeId == null
                ? puduReportRepository.findOverallSummary(start, end)
                : puduReportRepository.findOverallSummaryByStore(storeId, start, end);

        int totalJobCount = (int) summary.getTotalJobCount();
        long totalCleanTimeSeconds = (long) summary.getTotalCleanTimeSeconds();
        double totalCleanArea = summary.getTotalCleanArea();
        double avgBattery = summary.getAvgBattery();
        long totalWaterMl = summary.getTotalWaterMl();

        double avgCleanTimeMinutes = totalJobCount > 0 ? (totalCleanTimeSeconds / 60.0) / totalJobCount : 0;
        double avgCleanAreaVal = totalJobCount > 0 ? totalCleanArea / totalJobCount : 0;
        double totalHours = totalCleanTimeSeconds / 3600.0;
        double areaPerHour = totalHours > 0 ? totalCleanArea / totalHours : 0;

        // 상태별 집계
        List<StatusCountDTO> statusRows = storeId == null
                ? puduReportRepository.findStatusCounts(start, end)
                : puduReportRepository.findStatusCountsByStore(storeId, start, end);

        List<StatusEntry> statusEntries = statusRows.stream()
                .map(row -> {
                    int status = row.getStatus() != null ? row.getStatus() : 0;
                    int count = (int) row.getCount();
                    String label = STATUS_LABEL_MAP.getOrDefault(status, "기타");
                    return StatusEntry.builder()
                            .label(label)
                            .count(count)
                            .percent(totalJobCount > 0 ? (count * 100.0) / totalJobCount : 0)
                            .build();
                })
                .toList();

        // 같은 라벨 병합 (기타가 status 0,1에 매핑되므로)
        Map<String, StatusEntry> mergedStatus = new LinkedHashMap<>();
        for (StatusEntry entry : statusEntries) {
            mergedStatus.merge(entry.getLabel(), entry,
                    (a, b) -> StatusEntry.builder()
                            .label(a.getLabel())
                            .count(a.getCount() + b.getCount())
                            .percent(totalJobCount > 0 ? ((a.getCount() + b.getCount()) * 100.0) / totalJobCount : 0)
                            .build());
        }
        statusEntries = new ArrayList<>(mergedStatus.values());

        // 로봇별 집계
        List<RobotStatsDTO> robotRows = storeId == null
                ? puduReportRepository.findRobotStats(start, end)
                : puduReportRepository.findRobotStatsByStore(storeId, start, end);

        List<RobotStats> robotStats = robotRows.stream()
                .map(row -> {
                    String nickname = row.getNickname() != null ? row.getNickname() : "Unknown";
                    int count = (int) row.getCount();
                    double waterSumLiter = row.getWaterSum() / 1000.0;
                    double successRate = count > 0 ? (row.getSuccessCount() * 100.0) / count : 0;

                    return RobotStats.builder()
                            .nickname(nickname)
                            .count(count)
                            .areaSum(row.getAreaSum())
                            .avgBattery(row.getAvgBattery())
                            .waterSumLiter(waterSumLiter)
                            .successRate(successRate)
                            .build();
                })
                .toList();

        // 구역별 집계
        List<ZoneStatsDTO> zoneRows = storeId == null
                ? puduReportRepository.findZoneStats(start, end)
                : puduReportRepository.findZoneStatsByStore(storeId, start, end);

        List<ZoneStats> zoneStats = zoneRows.stream()
                .map(row -> {
                    String mapName = row.getMapName() != null ? row.getMapName() : "Unknown";
                    int count = (int) row.getCount();
                    double avgTime = row.getAvgCleanTime() / 60.0; // 초→분

                    return ZoneStats.builder()
                            .mapName(mapName)
                            .count(count)
                            .areaSum(row.getAreaSum())
                            .avgCleanTimeMinutes(avgTime)
                            .build();
                })
                .toList();

        // remarks
        List<RemarkDTO> remarkRows = storeId == null
                ? puduReportRepository.findRemarks(start, end)
                : puduReportRepository.findRemarksByStore(storeId, start, end);

        List<RemarkEntry> remarks = remarkRows.stream()
                .map(row -> RemarkEntry.builder()
                        .date(row.getStartTime() != null ? row.getStartTime().format(DATE_FMT) : "날짜 없음")
                        .remark(row.getRemark())
                        .build())
                .toList();

        return ReportStatistics.builder()
                .totalJobCount(totalJobCount)
                .totalCleanTimeSeconds(totalCleanTimeSeconds)
                .totalCleanArea(totalCleanArea)
                .avgBatteryPercent(avgBattery)
                .totalWaterMl(totalWaterMl)
                .robotStats(robotStats)
                .zoneStats(zoneStats)
                .statusEntries(statusEntries)
                .avgCleanTimeMinutes(avgCleanTimeMinutes)
                .avgCleanArea(avgCleanAreaVal)
                .areaPerHour(areaPerHour)
                .remarks(remarks)
                .build();
    }

    /**
     * Java 순회 기반 통계 계산 (기존 방식, 성능 비교 테스트용으로 보존)
     */
    public ReportStatistics computeFromList(List<PuduReportDTO> data) {
        int totalJobCount = data.size();

        long totalCleanTimeSeconds = 0;
        double totalCleanArea = 0;
        long totalCostBattery = 0;
        long totalCostWater = 0;

        for (PuduReportDTO row : data) {
            totalCleanTimeSeconds += row.getCleanTime() != null ? row.getCleanTime().longValue() : 0;
            totalCleanArea += row.getCleanArea() != null ? row.getCleanArea().doubleValue() : 0;
            totalCostBattery += row.getCostBattery() != null ? row.getCostBattery() : 0;
            totalCostWater += row.getCostWater() != null ? row.getCostWater() : 0;
        }

        double avgBattery = totalJobCount > 0 ? (double) totalCostBattery / totalJobCount : 0;
        double avgCleanTimeMinutes = totalJobCount > 0 ? (totalCleanTimeSeconds / 60.0) / totalJobCount : 0;
        double avgCleanArea = totalJobCount > 0 ? totalCleanArea / totalJobCount : 0;
        double totalHours = totalCleanTimeSeconds / 3600.0;
        double areaPerHour = totalHours > 0 ? totalCleanArea / totalHours : 0;

        Map<String, List<PuduReportDTO>> byRobot = data.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getNickname() != null ? r.getNickname() : "Unknown",
                        LinkedHashMap::new, Collectors.toList()));

        List<RobotStats> robotStats = byRobot.entrySet().stream().map(e -> {
            List<PuduReportDTO> rows = e.getValue();
            int count = rows.size();
            double area = rows.stream().mapToDouble(r -> r.getCleanArea() != null ? r.getCleanArea() : 0).sum();
            double bat = count > 0 ? rows.stream().mapToLong(r -> r.getCostBattery() != null ? r.getCostBattery() : 0).sum() / (double) count : 0;
            double water = rows.stream().mapToLong(r -> r.getCostWater() != null ? r.getCostWater() : 0).sum() / 1000.0;
            long success = rows.stream().filter(r -> r.getStatus() != null && r.getStatus() == 4).count();
            double successRate = count > 0 ? (success * 100.0) / count : 0;

            return RobotStats.builder()
                    .nickname(e.getKey())
                    .count(count)
                    .areaSum(area)
                    .avgBattery(bat)
                    .waterSumLiter(water)
                    .successRate(successRate)
                    .build();
        }).toList();

        Map<String, List<PuduReportDTO>> byZone = data.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getMapName() != null ? r.getMapName() : "Unknown",
                        LinkedHashMap::new, Collectors.toList()));

        List<ZoneStats> zoneStats = byZone.entrySet().stream().map(e -> {
            List<PuduReportDTO> rows = e.getValue();
            int count = rows.size();
            double area = rows.stream().mapToDouble(r -> r.getCleanArea() != null ? r.getCleanArea() : 0).sum();
            double avgTime = count > 0
                    ? rows.stream().mapToDouble(r -> r.getCleanTime() != null ? r.getCleanTime() / 60.0 : 0).sum() / count
                    : 0;

            return ZoneStats.builder()
                    .mapName(e.getKey())
                    .count(count)
                    .areaSum(area)
                    .avgCleanTimeMinutes(avgTime)
                    .build();
        }).toList();

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        for (PuduReportDTO row : data) {
            String label = STATUS_LABEL_MAP.getOrDefault(
                    row.getStatus() != null ? row.getStatus() : 0, "기타");
            statusCounts.merge(label, 1, Integer::sum);
        }

        List<StatusEntry> statusEntries = statusCounts.entrySet().stream()
                .map(e -> StatusEntry.builder()
                        .label(e.getKey())
                        .count(e.getValue())
                        .percent(totalJobCount > 0 ? (e.getValue() * 100.0) / totalJobCount : 0)
                        .build())
                .toList();

        List<RemarkEntry> remarks = data.stream()
                .filter(r -> r.getRemark() != null && !r.getRemark().isBlank() && !"null".equals(r.getRemark()))
                .map(r -> RemarkEntry.builder()
                        .date(r.getStartTime() != null ? r.getStartTime().format(DATE_FMT) : "날짜 없음")
                        .remark(r.getRemark())
                        .build())
                .toList();

        return ReportStatistics.builder()
                .totalJobCount(totalJobCount)
                .totalCleanTimeSeconds(totalCleanTimeSeconds)
                .totalCleanArea(totalCleanArea)
                .avgBatteryPercent(avgBattery)
                .totalWaterMl(totalCostWater)
                .robotStats(robotStats)
                .zoneStats(zoneStats)
                .statusEntries(statusEntries)
                .avgCleanTimeMinutes(avgCleanTimeMinutes)
                .avgCleanArea(avgCleanArea)
                .areaPerHour(areaPerHour)
                .remarks(remarks)
                .build();
    }
}
