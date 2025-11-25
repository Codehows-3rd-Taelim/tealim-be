package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


// 없어짐
@RestController
@RequestMapping("/api/robot")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Server running. Cleaning Report API Ready.");
    }

    @GetMapping("/report/list")
    public ResponseEntity<String> getCleanReportList(
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam Long shop_id) {
        return reportService.getCleanReportList(start_time, end_time, offset, limit, timezone_offset, shop_id);
    }

    @GetMapping("/report/detail")
    public ResponseEntity<String> getCleanReportDetail(
            @RequestParam String sn,
            @RequestParam String report_id,
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id) {
        return reportService.getCleanReportDetail(sn, report_id, start_time, end_time, timezone_offset, shop_id);
    }
}