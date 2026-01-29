package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.ai.dto.*;
import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuduReportRepository extends JpaRepository<PuduReport, Long> {


    List<PuduReport> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );
    Optional<PuduReport> findByReportId(Long reportId);


    List<PuduReport> findByRobot_SnAndStartTimeBetween(
            String sn,
            LocalDateTime start,
            LocalDateTime end,
            Sort sort
    );

    Page<PuduReport> findByRobot_SnAndStartTimeBetween(
            String sn,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    Page<PuduReport> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    List<PuduReport> findAllByRobot_RobotIdInAndStartTimeBetween(
            List<Long> robotIds,
            LocalDateTime start,
            LocalDateTime end,
            Sort sort
    );

    Page<PuduReport> findAllByRobot_RobotIdInAndStartTimeBetween(
            List<Long> robotIds,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    List<PuduReport> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end,
            Sort sort
    );

    @Query("""
        select pr
        from PuduReport pr
        join pr.robot r
        join r.store s
        where s.storeId = :storeId
          and pr.startTime >= :start
          and pr.startTime < :end
    """)
    List<PuduReport> findByStoreIdAndPeriod(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // ===== 집계 쿼리 (전매장) =====

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.OverallSummaryDTO(
            COUNT(pr), COALESCE(SUM(pr.cleanTime),0), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.costBattery),0), COALESCE(SUM(pr.costWater),0))
        FROM PuduReport pr
        WHERE pr.startTime >= :start AND pr.startTime < :end
    """)
    OverallSummaryDTO findOverallSummary(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.StatusCountDTO(pr.status, COUNT(pr))
        FROM PuduReport pr
        WHERE pr.startTime >= :start AND pr.startTime < :end
        GROUP BY pr.status
    """)
    List<StatusCountDTO> findStatusCounts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.RobotStatsDTO(
            r.nickname, COUNT(pr), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.costBattery),0), COALESCE(SUM(pr.costWater),0),
            SUM(CASE WHEN pr.status = 4 THEN 1 ELSE 0 END))
        FROM PuduReport pr JOIN pr.robot r
        WHERE pr.startTime >= :start AND pr.startTime < :end
        GROUP BY r.nickname
    """)
    List<RobotStatsDTO> findRobotStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.ZoneStatsDTO(
            pr.mapName, COUNT(pr), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.cleanTime),0))
        FROM PuduReport pr
        WHERE pr.startTime >= :start AND pr.startTime < :end
        GROUP BY pr.mapName
    """)
    List<ZoneStatsDTO> findZoneStats(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.RemarkDTO(pr.startTime, pr.remark)
        FROM PuduReport pr
        WHERE pr.startTime >= :start AND pr.startTime < :end
          AND pr.remark IS NOT NULL AND pr.remark <> '' AND pr.remark <> 'null'
    """)
    List<RemarkDTO> findRemarks(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ===== 집계 쿼리 (매장별) =====

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.OverallSummaryDTO(
            COUNT(pr), COALESCE(SUM(pr.cleanTime),0), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.costBattery),0), COALESCE(SUM(pr.costWater),0))
        FROM PuduReport pr JOIN pr.robot r JOIN r.store s
        WHERE s.storeId = :storeId AND pr.startTime >= :start AND pr.startTime < :end
    """)
    OverallSummaryDTO findOverallSummaryByStore(@Param("storeId") Long storeId,
                                       @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.StatusCountDTO(pr.status, COUNT(pr))
        FROM PuduReport pr JOIN pr.robot r JOIN r.store s
        WHERE s.storeId = :storeId AND pr.startTime >= :start AND pr.startTime < :end
        GROUP BY pr.status
    """)
    List<StatusCountDTO> findStatusCountsByStore(@Param("storeId") Long storeId,
                                           @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.RobotStatsDTO(
            r.nickname, COUNT(pr), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.costBattery),0), COALESCE(SUM(pr.costWater),0),
            SUM(CASE WHEN pr.status = 4 THEN 1 ELSE 0 END))
        FROM PuduReport pr JOIN pr.robot r JOIN r.store s
        WHERE s.storeId = :storeId AND pr.startTime >= :start AND pr.startTime < :end
        GROUP BY r.nickname
    """)
    List<RobotStatsDTO> findRobotStatsByStore(@Param("storeId") Long storeId,
                                         @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.ZoneStatsDTO(
            pr.mapName, COUNT(pr), COALESCE(SUM(pr.cleanArea),0),
            COALESCE(AVG(pr.cleanTime),0))
        FROM PuduReport pr JOIN pr.robot r JOIN r.store s
        WHERE s.storeId = :storeId AND pr.startTime >= :start AND pr.startTime < :end
        GROUP BY pr.mapName
    """)
    List<ZoneStatsDTO> findZoneStatsByStore(@Param("storeId") Long storeId,
                                        @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        SELECT new com.codehows.taelimbe.ai.dto.RemarkDTO(pr.startTime, pr.remark)
        FROM PuduReport pr JOIN pr.robot r JOIN r.store s
        WHERE s.storeId = :storeId AND pr.startTime >= :start AND pr.startTime < :end
          AND pr.remark IS NOT NULL AND pr.remark <> '' AND pr.remark <> 'null'
    """)
    List<RemarkDTO> findRemarksByStore(@Param("storeId") Long storeId,
                                      @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
