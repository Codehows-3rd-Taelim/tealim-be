package com.codehows.taelimbe.ai.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OverallSummaryDTO {
    private final long totalJobCount;
    private final double totalCleanTimeSeconds;
    private final double totalCleanArea;
    private final double avgBattery;
    private final long totalWaterMl;
}
