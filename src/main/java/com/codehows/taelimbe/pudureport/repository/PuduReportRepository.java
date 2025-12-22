package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

}
