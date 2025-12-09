package com.codehows.taelimbe.report.repository;

import com.codehows.taelimbe.ai.repository.MapStatsProjection;
import com.codehows.taelimbe.ai.repository.ReportSummaryProjection;
import com.codehows.taelimbe.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByRobot_Sn(String sn);

    List<Report> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Report> findByTaskId(Long taskId);

    /**
     * 기간별 전체 보고서 운영 데이터 요약 (AI 보고서 1. 장비 운영 요약)
     * JPQL은 엔티티 필드명(cleanTime, taskArea 등)을 사용합니다.
     */
    @Query(value = """
        SELECT 
            COUNT(r.reportId) AS totalTaskCount,
            SUM(r.cleanTime) AS totalCleanTime,
            SUM(r.taskArea) AS totalTaskArea,
            SUM(r.cleanArea) AS totalCleanArea,
            SUM(r.costWater) AS totalCostWater,
            SUM(r.costBattery) AS totalCostBattery
        FROM Report r
        WHERE r.startTime BETWEEN :start AND :end
    """)
    Optional<ReportSummaryProjection> summarizeReportByTimeRange(
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 기간별 맵(Map) 기준 그룹화된 통계 (AI 보고서 3. 층별 작업 현황)
     * JPQL은 엔티티 필드명(mapName, cleanArea 등)을 사용합니다.
     */
    @Query(value = """
        SELECT
            r.mapName AS mapName,
            COUNT(r.reportId) AS taskCount,
            SUM(r.cleanArea) AS cleanArea,
            SUM(r.costBattery) AS costBattery,
            SUM(r.costWater) AS costWater
        FROM Report r
        WHERE r.startTime BETWEEN :start AND :end
        GROUP BY r.mapName
        ORDER BY taskCount DESC
    """)
    List<MapStatsProjection> summarizeMapStatsByTimeRange(
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 기간별 작업 상태 (Status) 카운트 (AI 보고서 4. 작업 실패 및 중단 현황)
     */
    @Query(value = """
        SELECT
            r.status AS status,
            COUNT(r.reportId) AS count
        FROM Report r
        WHERE r.startTime BETWEEN :start AND :end
        GROUP BY r.status
    """)
    List<Object[]> countStatusByTimeRange(
            LocalDateTime start,
            LocalDateTime end
    );
}

