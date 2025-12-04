package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PuduReportRepository extends JpaRepository<PuduReport, Long> {
    List<PuduReport> findByRobot_Sn(String sn);

    List<PuduReport> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<PuduReport> findByReportId(Long reportId);
}