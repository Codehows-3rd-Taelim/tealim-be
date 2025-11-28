package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.entity.Report;
import com.codehows.taelimbe.entity.Robot;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {

    private Long reprotId;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Float cleanTime;

    private Float taskArea;

    private Float cleanArea;

    private Integer mode;

    private Long costBattery;

    private Long costWater;

    private String mapName;

    private String mapUrl;

    private Robot robot;

    public static ReportDTO createReportDTO(Report report) {
        return ReportDTO.builder()
                .reprotId(report.getReportId())
                .status(report.getStatus())
                .startTime(report.getStartTime())
                .endTime(report.getEndTime())
                .cleanTime(report.getCleanTime())
                .taskArea(report.getTaskArea())
                .cleanArea(report.getCleanArea())
                .mode(report.getMode())
                .costBattery(report.getCostBattery())
                .costWater(report.getCostWater())
                .mapName(report.getMapName())
                .mapUrl(report.getMapUrl())
                .robot(report.getRobot())
                .build();
    }
}
