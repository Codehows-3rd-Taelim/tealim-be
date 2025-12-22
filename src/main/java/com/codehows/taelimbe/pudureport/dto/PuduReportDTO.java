package com.codehows.taelimbe.pudureport.dto;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuduReportDTO {

    private Long puduReportId;
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
    private String nickname;

    public static PuduReportDTO createReportDTO(PuduReport puduReport) {
        return PuduReportDTO.builder()
                .puduReportId(puduReport.getPuduReportId())
                .reportId(puduReport.getReportId())
                .status(puduReport.getStatus())
                .startTime(puduReport.getStartTime())
                .endTime(puduReport.getEndTime())
                .cleanTime(puduReport.getCleanTime())
                .taskArea(puduReport.getTaskArea())
                .cleanArea(puduReport.getCleanArea())
                .mode(puduReport.getMode())
                .costBattery(puduReport.getCostBattery())
                .costWater(puduReport.getCostWater())
                .mapName(puduReport.getMapName())
                .mapUrl(puduReport.getMapUrl())
                .robotId(puduReport.getRobot().getRobotId())
                .nickname(puduReport.getRobot().getNickname())
                .build();
    }
}