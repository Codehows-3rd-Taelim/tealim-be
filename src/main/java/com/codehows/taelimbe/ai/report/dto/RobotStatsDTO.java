package com.codehows.taelimbe.ai.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RobotStatsDTO {
    private final String nickname;
    private final long count;
    private final double areaSum;
    private final double avgBattery;
    private final long waterSum;
    private final long successCount;
}
