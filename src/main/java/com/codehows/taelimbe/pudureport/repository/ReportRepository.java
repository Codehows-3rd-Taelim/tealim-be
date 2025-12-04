package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.pudureport.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByRobot_Sn(String sn);

    List<Report> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Report> findByReportId(Long reportId);
}