package com.codehows.taelimbe.repository;

import com.codehows.taelimbe.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByRobot_Sn(String sn);
}

    List<Report> findByReportDateBetween(LocalDateTime start, LocalDateTime end);

}