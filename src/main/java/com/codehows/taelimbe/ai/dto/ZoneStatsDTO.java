package com.codehows.taelimbe.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ZoneStatsDTO {
    private final String mapName;
    private final long count;
    private final double areaSum;
    private final double avgCleanTime;
}
