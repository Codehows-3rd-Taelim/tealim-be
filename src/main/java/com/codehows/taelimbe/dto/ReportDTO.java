package com.codehows.taelimbe.dto;

import com.codehows.taelimbe.entity.Report;
import com.codehows.taelimbe.entity.Robot;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {

    private Long reportId;

    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Float cleanTime;
    private Float taskArea;
    private Float cleanArea;

    private Integer mode;
    private Long costBattery;
    private Long costWater;

    private String mapName;
    private String mapUrl;

    private Long robotId;
    private String robotSn;


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
