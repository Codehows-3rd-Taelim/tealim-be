package com.codehows.taelimbe.ai.dto;

public class ReportResult {
    private final String jsonReport;
    private final String startDate;
    private final String endDate;

    public ReportResult(String jsonReport, String startDate, String endDate) {
        this.jsonReport = jsonReport;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getJsonReport() { return jsonReport; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
}
