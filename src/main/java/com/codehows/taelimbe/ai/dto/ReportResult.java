package com.codehows.taelimbe.ai.dto;

import lombok.Getter;

@Getter
public class ReportResult {
    private final String rawReport;
    private final String startDate;
    private final String endDate;

    public ReportResult(String rawReport, String startDate, String endDate) {
        this.rawReport = rawReport;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}