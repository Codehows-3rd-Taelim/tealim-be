package com.codehows.taelimbe.ai.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class ReportStatistics {
    private int totalJobCount;
    private long totalCleanTimeSeconds;
    private double totalCleanArea;
    private double avgBatteryPercent;
    private long totalWaterMl;
    private List<RobotStats> robotStats;
    private List<ZoneStats> zoneStats;
    private List<StatusEntry> statusEntries;
    private double avgCleanTimeMinutes;
    private double avgCleanArea;
    private double areaPerHour;
    private List<RemarkEntry> remarks;

    @Getter
    @Builder
    public static class RobotStats {
        private String nickname;
        private int count;
        private double areaSum;
        private double avgBattery;
        private double waterSumLiter;
        private double successRate;
    }

    @Getter
    @Builder
    public static class ZoneStats {
        private String mapName;
        private int count;
        private double areaSum;
        private double avgCleanTimeMinutes;
    }

    @Getter
    @Builder
    public static class StatusEntry {
        private String label;
        private int count;
        private double percent;
    }

    @Getter
    @Builder
    public static class RemarkEntry {
        private String date;
        private String remark;
    }

    /**
     * LLM에 전달할 통계 요약 텍스트 생성
     */
    public String toSummaryText() {
        long hours = totalCleanTimeSeconds / 3600;
        long minutes = (totalCleanTimeSeconds % 3600) / 60;
        double waterLiter = totalWaterMl / 1000.0;

        // 정상 완료율, 취소율
        double completionRate = 0;
        double cancelRate = 0;
        int remarkCount = remarks != null ? remarks.size() : 0;

        for (StatusEntry entry : statusEntries) {
            if ("정상 완료".equals(entry.getLabel())) {
                completionRate = entry.getPercent();
            } else if ("취소".equals(entry.getLabel())) {
                cancelRate = entry.getPercent();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("총 작업 횟수: %d회, ", totalJobCount));
        sb.append(String.format("총 작업 시간: %d시간 %d분, ", hours, minutes));
        sb.append(String.format("총 청소 면적: %.1f㎡, ", totalCleanArea));
        sb.append(String.format("평균 배터리 소모: %.1f%%, ", avgBatteryPercent));
        sb.append(String.format("총 물 소비량: %.1fℓ, ", waterLiter));
        sb.append(String.format("정상 완료율: %.1f%%, ", completionRate));
        sb.append(String.format("취소율: %.1f%%, ", cancelRate));
        sb.append(String.format("특이사항: %d건", remarkCount));

        if (robotStats != null && !robotStats.isEmpty()) {
            sb.append("\n\n로봇별 요약: ");
            sb.append(robotStats.stream()
                    .map(r -> String.format("%s(%d회, 성공률 %.1f%%)", r.getNickname(), r.getCount(), r.getSuccessRate()))
                    .collect(Collectors.joining(", ")));
        }

        if (zoneStats != null && !zoneStats.isEmpty()) {
            sb.append("\n\n구역별 요약: ");
            sb.append(zoneStats.stream()
                    .map(z -> String.format("%s(%d회, %.1f㎡)", z.getMapName(), z.getCount(), z.getAreaSum()))
                    .collect(Collectors.joining(", ")));
        }

        sb.append(String.format("\n\n효율 메트릭: 평균 청소 시간 %.1f분, 평균 청소 면적 %.1f㎡, 시간당 청소 효율 %.1f㎡/시간",
                avgCleanTimeMinutes, avgCleanArea, areaPerHour));

        return sb.toString();
    }
}
