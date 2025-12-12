package com.codehows.taelimbe.ai.repository;

public interface MapStatsProjection {


    String getMapName();
    Long getTaskCount();
    Double getCleanArea();
    Long getCostBattery();
    Long getCostWater();
}