package com.codehows.taelimbe.ai.service;

import com.codehows.taelimbe.ai.dto.ReportStatistics;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 단위 테스트: Java 순회 기반 computeFromList() 성능 측정.
 * DB 집계 방식은 Java 측 계산이 나눗셈/매핑뿐이므로 "~0ms"로 간주.
 */
class ReportStatisticsPerformanceTest {

    private ReportStatisticsService service;
    private final Random random = new Random(42);

    private static final String[] NICKNAMES = {"청소봇A", "청소봇B", "청소봇C", "청소봇D"};
    private static final String[] MAP_NAMES = {"1층 MDCG", "1층 생산3과", "2층 사무실", "3층 회의실"};
    private static final int[] STATUSES = {0, 1, 2, 3, 4, 4, 4, 4, 5, 6}; // 4가 많음 (정상 완료)

    @BeforeEach
    void setUp() {
        // ReportStatisticsService에 PuduReportRepository가 필요하지만
        // computeFromList()는 DB 의존 없음 → null 전달 가능
        service = new ReportStatisticsService(null);
    }

    @Test
    void benchmark_computeFromList() {
        System.out.println("=== Java 순회 기반 computeFromList() 성능 벤치마크 ===");
        System.out.println();

        for (int n : List.of(100, 500, 1000, 5000)) {
            List<PuduReportDTO> data = generateMockData(n);

            // Warmup (JIT 최적화)
            for (int i = 0; i < 5; i++) {
                service.computeFromList(data);
            }

            // 메모리 측정 준비
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}

            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            // 실행 시간 측정 (10회 평균)
            int iterations = 10;
            long startNano = System.nanoTime();
            ReportStatistics result = null;
            for (int i = 0; i < iterations; i++) {
                result = service.computeFromList(data);
            }
            long elapsed = (System.nanoTime() - startNano) / iterations;

            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memUsed = memAfter - memBefore;

            // 결과 출력
            System.out.printf("N=%5d → Java compute: %8.2f ms | 메모리 변화: %+d KB%n",
                    n, elapsed / 1_000_000.0, memUsed / 1024);

            // 결과 정확성 검증
            assertNotNull(result);
            assertEquals(n, result.getTotalJobCount());
            assertTrue(result.getTotalCleanArea() > 0);
            assertTrue(result.getAvgBatteryPercent() >= 0);
            assertFalse(result.getRobotStats().isEmpty());
            assertFalse(result.getZoneStats().isEmpty());
            assertFalse(result.getStatusEntries().isEmpty());
        }

        System.out.println();
        System.out.println("※ DB 집계 방식의 Java 측 처리: ~0ms (나눗셈/매핑만 수행)");
        System.out.println("※ DB 집계 방식의 실제 성능은 통합 테스트(ReportStatisticsIntegrationTest)에서 측정");
    }

    @Test
    void verify_computeFromList_accuracy() {
        List<PuduReportDTO> data = generateMockData(200);
        ReportStatistics stats = service.computeFromList(data);

        // 총 작업 횟수 = row 수
        assertEquals(200, stats.getTotalJobCount());

        // 로봇별 합계 = 총 작업 횟수
        int robotTotal = stats.getRobotStats().stream().mapToInt(ReportStatistics.RobotStats::getCount).sum();
        assertEquals(200, robotTotal);

        // 구역별 합계 = 총 작업 횟수
        int zoneTotal = stats.getZoneStats().stream().mapToInt(ReportStatistics.ZoneStats::getCount).sum();
        assertEquals(200, zoneTotal);

        // 상태별 합계 = 총 작업 횟수
        int statusTotal = stats.getStatusEntries().stream().mapToInt(ReportStatistics.StatusEntry::getCount).sum();
        assertEquals(200, statusTotal);

        // 비율 합계 ≈ 100%
        double percentSum = stats.getStatusEntries().stream().mapToDouble(ReportStatistics.StatusEntry::getPercent).sum();
        assertEquals(100.0, percentSum, 0.1);

        // toSummaryText 생성 가능
        String summaryText = stats.toSummaryText();
        assertNotNull(summaryText);
        assertTrue(summaryText.contains("총 작업 횟수: 200회"));
    }

    private List<PuduReportDTO> generateMockData(int count) {
        List<PuduReportDTO> data = new ArrayList<>(count);
        LocalDateTime baseTime = LocalDateTime.of(2025, 6, 1, 8, 0);

        for (int i = 0; i < count; i++) {
            String nickname = NICKNAMES[random.nextInt(NICKNAMES.length)];
            String mapName = MAP_NAMES[random.nextInt(MAP_NAMES.length)];
            int status = STATUSES[random.nextInt(STATUSES.length)];
            float cleanTime = 300 + random.nextFloat() * 3600; // 5분~65분 (초)
            float cleanArea = 50 + random.nextFloat() * 500;   // 50~550㎡
            long costBattery = 10 + random.nextInt(50);         // 10~60%
            long costWater = 500 + random.nextInt(5000);        // 500~5500ml
            String remark = (i % 50 == 0) ? "장애물 감지" : null; // 2%만 remark

            data.add(PuduReportDTO.builder()
                    .puduReportId((long) i)
                    .reportId((long) (10000 + i))
                    .status(status)
                    .startTime(baseTime.plusMinutes(i * 10L))
                    .endTime(baseTime.plusMinutes(i * 10L + (long) (cleanTime / 60)))
                    .cleanTime(cleanTime)
                    .cleanArea(cleanArea)
                    .costBattery(costBattery)
                    .costWater(costWater)
                    .mapName(mapName)
                    .nickname(nickname)
                    .remark(remark)
                    .build());
        }
        return data;
    }
}
