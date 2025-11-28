package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.CleaningLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


// 없어짐
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/robot")
public class CleaningLogController {

    private final CleaningLogService cleaningLogService;



    @GetMapping("/report/list")
    public ResponseEntity<String> getCleanReportList(
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam Long shop_id) {
        return cleaningLogService.getCleanReportList(start_time, end_time, offset, limit, timezone_offset, shop_id);
    }

    @GetMapping("/report/detail")
    public ResponseEntity<String> getCleanReportDetail(
            @RequestParam String sn,
            @RequestParam String report_id,
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id) {
        return cleaningLogService.getCleanReportDetail(sn, report_id, start_time, end_time, timezone_offset, shop_id);
    }
}