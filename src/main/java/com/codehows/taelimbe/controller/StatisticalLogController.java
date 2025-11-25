package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.StatisticalLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/robot")
public class StatisticalLogController {

    @Autowired
    private StatisticalLogService statisticalLogService;

    @GetMapping("/charging/list")
    public ResponseEntity<String> getChargingRecordList(
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id) {
        return statisticalLogService.getChargingRecordList(start_time, end_time, offset, limit, timezone_offset, shop_id);
    }
}