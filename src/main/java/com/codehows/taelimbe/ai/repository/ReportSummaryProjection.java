package com.codehows.taelimbe.ai.repository;


public interface ReportSummaryProjection {

    Long getTotalTaskCount();
    Double getTotalCleanTime();
    Double getTotalTaskArea();
    Double getTotalCleanArea();
    Long getTotalCostWater();
    Long getTotalCostBattery();
}