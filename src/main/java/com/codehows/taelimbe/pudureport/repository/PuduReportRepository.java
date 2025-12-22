package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuduReportRepository extends JpaRepository<PuduReport, Long> {
    List<PuduReport> findByRobot_Sn(String sn);

    List<PuduReport> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );
    Optional<PuduReport> findByReportId(Long reportId);

    List<PuduReport> findAllByRobot_RobotIdIn(List<Long> robotIds);

    List<PuduReport> findAllByRobot_RobotIdInAndStartTimeBetween(
            List<Long> robotIds,
            LocalDateTime start,
            LocalDateTime end
    );

    Page<PuduReport> findByStartTimeBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    Page<PuduReport> findAllByRobot_RobotIdInAndStartTimeBetween(
            List<Long> robotIds,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    @Query("""
        select pr
        from PuduReport pr
        join pr.robot r
        join r.store s
        where s.storeId = :storeId
          and pr.startTime >= :start
          and pr.endTime <= :end
    """)
    List<PuduReport> findByStoreIdAndPeriod(
            @Param("storeId") Long storeId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
