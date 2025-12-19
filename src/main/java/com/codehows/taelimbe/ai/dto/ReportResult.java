package com.codehows.taelimbe.ai.dto;

public class ReportResult {
    private final String rawReport;
    private final String startDate;
    private final String endDate;

    public ReportResult(String rawReport, String startDate, String endDate) {
        this.rawReport = rawReport;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String rawReport() { return rawReport; }
    public String startDate() { return startDate; }
    public String endDate() { return endDate; }
}