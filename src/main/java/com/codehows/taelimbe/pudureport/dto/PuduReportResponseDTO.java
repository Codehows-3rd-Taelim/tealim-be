package com.codehows.taelimbe.pudureport.dto;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuduReportResponseDTO {

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

    private Long storeId;
    private Long robotId;
    private String sn;

    private String remark;

    public static PuduReportResponseDTO createReportResponseDTO(PuduReport puduReport) {
        return PuduReportResponseDTO.builder()
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
                .storeId(puduReport.getRobot().getStore().getStoreId())
                .robotId(puduReport.getRobot().getRobotId())
                .sn(puduReport.getRobot().getSn())
                .remark(puduReport.getRemark())
                .build();
    }
}