package com.codehows.taelimbe.report.repository;

import com.codehows.taelimbe.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByRobot_Sn(String sn);

    List<Report> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Report> findByTaskId(Long taskId);

    List<Report> findAllByRobot_RobotIdIn(List<Long> robotIds);

    List<Report> findAllByRobot_RobotIdInAndStartTimeBetween(
                                                            List<Long> robotIds,
                                                            LocalDateTime start,
                                                            LocalDateTime end
                                                    );
}
