package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.ReportStatistics;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: 실제 MySQL DB를 사용하여
 * 기존 Java 순회 방식 vs DB 집계 쿼리 방식 성능 비교.
 *
 * 실행 조건: MySQL이 실행 중이고, pudu_report 테이블에 데이터가 존재해야 합니다.
 */
@SpringBootTest
class ReportStatisticsIntegrationTest {

    @Autowired
    private PuduReportRepository puduReportRepository;

    @Autowired
    private ReportStatisticsService reportStatisticsService;

    @Test
    void benchmark_oldVsNew_allStores() {
        // 6개월 범위 (데이터가 있는 기간으로 조정 필요)
        LocalDateTime start = LocalDate.of(2025, 8, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(2026, 2, 1).atStartOfDay();

        System.out.println("=== 전매장 보고서 성능 비교 (Old vs New) ===");
        System.out.printf("기간: %s ~ %s%n%n", start.toLocalDate(), end.toLocalDate());

        // === Old: 전체 row 조회 + Java 계산 ===
        System.gc();
        long memBefore1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long t1 = System.nanoTime();

        List<PuduReport> allReports = puduReportRepository.findByStartTimeBetween(start, end);
        List<PuduReportDTO> dtos = allReports.stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
        ReportStatistics oldStats = reportStatisticsService.computeFromList(dtos);

        long oldTime = System.nanoTime() - t1;
        long memAfter1 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long oldMemUsed = memAfter1 - memBefore1;

        // === New: DB 집계 쿼리 ===
        System.gc();
        long memBefore2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long t2 = System.nanoTime();

        ReportStatistics newStats = reportStatisticsService.compute(start, end, null);

        long newTime = System.nanoTime() - t2;
        long memAfter2 = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long newMemUsed = memAfter2 - memBefore2;

        // === 결과 출력 ===
        int rowCount = dtos.size();
        System.out.printf("조회된 Row 수: %d건%n%n", rowCount);

        System.out.println("┌──────────────────┬───────────────┬───────────────┐");
        System.out.println("│ 항목             │ Old (Java)    │ New (DB집계)  │");
        System.out.println("├──────────────────┼───────────────┼───────────────┤");
        System.out.printf("│ 실행 시간        │ %9.2f ms  │ %9.2f ms  │%n",
                oldTime / 1_000_000.0, newTime / 1_000_000.0);
        System.out.printf("│ 메모리 변화      │ %+8d KB   │ %+8d KB   │%n",
                oldMemUsed / 1024, newMemUsed / 1024);
        System.out.printf("│ DB 전송 row 수   │ %9d     │ %9s     │%n",
                rowCount, "집계결과만");
        System.out.println("└──────────────────┴───────────────┴───────────────┘");
        System.out.println();

        double speedup = oldTime > 0 && newTime > 0 ? (double) oldTime / newTime : 0;
        System.out.printf("Speedup: %.1fx 빠름%n", speedup);
        System.out.printf("메모리 절감: %+d KB%n", (oldMemUsed - newMemUsed) / 1024);

        // === 결과 정확성 검증 ===
        System.out.println();
        System.out.println("=== 결과 일치 검증 ===");

        assertEquals(oldStats.getTotalJobCount(), newStats.getTotalJobCount(),
                "총 작업 횟수 불일치");
        System.out.printf("총 작업 횟수: Old=%d, New=%d ✓%n",
                oldStats.getTotalJobCount(), newStats.getTotalJobCount());

        assertEquals(oldStats.getTotalCleanArea(), newStats.getTotalCleanArea(), 1.0,
                "총 청소 면적 불일치");
        System.out.printf("총 청소 면적: Old=%.1f, New=%.1f ✓%n",
                oldStats.getTotalCleanArea(), newStats.getTotalCleanArea());

        assertEquals(oldStats.getTotalCleanTimeSeconds(), newStats.getTotalCleanTimeSeconds(),
                "총 작업 시간 불일치");
        System.out.printf("총 작업 시간(초): Old=%d, New=%d ✓%n",
                oldStats.getTotalCleanTimeSeconds(), newStats.getTotalCleanTimeSeconds());

        assertEquals(oldStats.getAvgBatteryPercent(), newStats.getAvgBatteryPercent(), 0.5,
                "평균 배터리 소모 불일치");
        System.out.printf("평균 배터리: Old=%.1f%%, New=%.1f%% ✓%n",
                oldStats.getAvgBatteryPercent(), newStats.getAvgBatteryPercent());

        assertEquals(oldStats.getTotalWaterMl(), newStats.getTotalWaterMl(),
                "총 물 소비량 불일치");
        System.out.printf("총 물 소비량(ml): Old=%d, New=%d ✓%n",
                oldStats.getTotalWaterMl(), newStats.getTotalWaterMl());

        // 로봇 수 일치
        assertEquals(oldStats.getRobotStats().size(), newStats.getRobotStats().size(),
                "로봇 수 불일치");
        System.out.printf("로봇 수: Old=%d, New=%d ✓%n",
                oldStats.getRobotStats().size(), newStats.getRobotStats().size());

        // 구역 수 일치
        assertEquals(oldStats.getZoneStats().size(), newStats.getZoneStats().size(),
                "구역 수 불일치");
        System.out.printf("구역 수: Old=%d, New=%d ✓%n",
                oldStats.getZoneStats().size(), newStats.getZoneStats().size());

        System.out.println();
        System.out.println("=== 모든 검증 통과 ===");
    }

    @Test
    void benchmark_repeated_runs() {
        LocalDateTime start = LocalDate.of(2025, 8, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(2026, 2, 1).atStartOfDay();

        System.out.println("=== 반복 실행 성능 비교 (5회 평균) ===");

        // Warmup
        reportStatisticsService.compute(start, end, null);
        List<PuduReport> warmup = puduReportRepository.findByStartTimeBetween(start, end);
        reportStatisticsService.computeFromList(
                warmup.stream().map(PuduReportDTO::createReportDTO).toList());

        int runs = 5;
        long oldTotal = 0, newTotal = 0;

        for (int i = 0; i < runs; i++) {
            // Old
            long t1 = System.nanoTime();
            List<PuduReport> all = puduReportRepository.findByStartTimeBetween(start, end);
            reportStatisticsService.computeFromList(
                    all.stream().map(PuduReportDTO::createReportDTO).toList());
            oldTotal += System.nanoTime() - t1;

            // New
            long t2 = System.nanoTime();
            reportStatisticsService.compute(start, end, null);
            newTotal += System.nanoTime() - t2;
        }

        double oldAvg = (oldTotal / runs) / 1_000_000.0;
        double newAvg = (newTotal / runs) / 1_000_000.0;
        double speedup = oldAvg / newAvg;

        System.out.printf("Old 평균: %.2f ms%n", oldAvg);
        System.out.printf("New 평균: %.2f ms%n", newAvg);
        System.out.printf("Speedup: %.1fx%n", speedup);
    }
}
