package com.codehows.taelimbe.dto;

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
}
