package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.ReportStatistics;
import com.codehows.taelimbe.ai.dto.ReportStatistics.*;
import org.springframework.stereotype.Component;

@Component
public class ReportMarkdownBuilder {

    public static final String INSIGHT_PLACEHOLDER = "{{INSIGHT}}";
    public static final String ANALYSIS_PLACEHOLDER = "{{ANALYSIS}}";

    public String build(ReportStatistics stats, String generatedDate,
                        String startDate, String endDate, String scopeSuffix) {

        StringBuilder md = new StringBuilder();

        // 제목
        md.append("# AI 청소로봇 관리 보고서 ").append(scopeSuffix).append("\n\n");

        // 기본 정보
        md.append("## 📋 보고서 기본 정보\n");
        md.append("- **제조사**: PUDU ROBOTICS\n");
        md.append("- **작성일**: ").append(generatedDate).append("\n");
        md.append("- **관리 기간**: ").append(startDate).append(" ~ ").append(endDate).append("\n\n");

        // AI 인사이트 (플레이스홀더)
        md.append("### 🤖 AI 운영 인사이트 요약\n\n");
        md.append(INSIGHT_PLACEHOLDER).append("\n\n");

        // 1. 전체 운영 요약
        long hours = stats.getTotalCleanTimeSeconds() / 3600;
        long minutes = (stats.getTotalCleanTimeSeconds() % 3600) / 60;
        double waterLiter = stats.getTotalWaterMl() / 1000.0;

        md.append("## 📊 1. 전체 운영 요약\n\n");
        md.append("| 항목 | 값 |\n");
        md.append("|------|------|\n");
        md.append(String.format("| 총 작업 횟수 | %d회 |\n", stats.getTotalJobCount()));
        md.append(String.format("| 총 작업 시간 | %d시간 %d분 |\n", hours, minutes));
        md.append(String.format("| 총 청소 면적 | %.1f ㎡ |\n", stats.getTotalCleanArea()));
        md.append(String.format("| 평균 배터리 소모 | %.1f%% |\n", stats.getAvgBatteryPercent()));
        md.append(String.format("| 총 물 소비량 | %.1f ℓ |\n", waterLiter));
        md.append("\n");

        // 2. 로봇별 작업 현황
        if (stats.getRobotStats() != null && !stats.getRobotStats().isEmpty()) {
            md.append("## 🤖 2. 로봇별 작업 현황\n\n");
            md.append("| 로봇 별명 | 작업 횟수 | 청소 면적(㎡) | 배터리 소모(%) | 물 소비량(L) | 성공률 |\n");
            md.append("|---------|-----------|---------------|----------------|--------------|--------|\n");
            for (RobotStats r : stats.getRobotStats()) {
                md.append(String.format("| %s | %d회 | %.1f㎡ | %.1f%% | %.1fL | %.1f%% |\n",
                        r.getNickname(), r.getCount(), r.getAreaSum(),
                        r.getAvgBattery(), r.getWaterSumLiter(), r.getSuccessRate()));
            }
            md.append("\n");
        }

        // 3. 층/구역별 작업 현황
        if (stats.getZoneStats() != null && !stats.getZoneStats().isEmpty()) {
            md.append("## 🏢 3. 층/구역별 작업 현황\n\n");
            md.append("| 층/구역 | 작업 횟수 | 청소 면적(㎡) | 평균 작업 시간(분) |\n");
            md.append("|---------|-----------|---------------|--------------------|");
            md.append("\n");
            for (ZoneStats z : stats.getZoneStats()) {
                md.append(String.format("| %s | %d회 | %.1f㎡ | %.1f분 |\n",
                        z.getMapName(), z.getCount(), z.getAreaSum(), z.getAvgCleanTimeMinutes()));
            }
            md.append("\n");
        }

        // 4. 작업 상태 분석
        if (stats.getStatusEntries() != null && !stats.getStatusEntries().isEmpty()) {
            md.append("## ✅ 4. 작업 상태 분석\n\n");
            for (StatusEntry s : stats.getStatusEntries()) {
                md.append(String.format("- **%s** : %d회 (%.1f%%)\n", s.getLabel(), s.getCount(), s.getPercent()));
            }
            md.append("\n");
        }

        // 5. 분석 및 권장사항 (플레이스홀더)
        md.append("## 💡 5. 분석 및 권장사항\n\n");
        md.append(ANALYSIS_PLACEHOLDER).append("\n\n");

        // 특이사항
        if (stats.getRemarks() != null && !stats.getRemarks().isEmpty()) {
            md.append("### 🔔 특이사항 요약\n\n");
            for (RemarkEntry r : stats.getRemarks()) {
                md.append(String.format("- [%s] %s\n", r.getDate(), r.getRemark()));
            }
            md.append("\n");
        }

        return md.toString();
    }
}
