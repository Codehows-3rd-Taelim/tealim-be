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
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id) {
        return statisticalLogService.getChargingRecordList(start_time, end_time, offset, limit, timezone_offset, shop_id);
    }

    // =============================
    // 🔋 Battery Health Status
    // =============================
    @GetMapping("/battery/list")
    public ResponseEntity<String> getBatteryHealthList(
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id,
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) Integer min_cycle,
            @RequestParam(required = false) Integer max_cycle,
            @RequestParam(required = false) Integer min_full_capacity,
            @RequestParam(required = false) Integer max_full_capacity
    ) {
        return statisticalLogService.getBatteryHealthList(
                start_time, end_time, offset, limit, timezone_offset,
                shop_id, sn,
                min_cycle, max_cycle,
                min_full_capacity, max_full_capacity
        );
    }

    // =============================
    // 🟦 Power-on Self-test (부팅 자가진단)
    // =============================
    @GetMapping("/boot/list")
    public ResponseEntity<String> getBootLogList(
            @RequestParam long start_time,
            @RequestParam long end_time,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(required = false) Long shop_id,
            @RequestParam(required = false) String check_step,
            @RequestParam(required = false) Integer is_success) {

        return statisticalLogService.getBootLogList(
                start_time, end_time, offset, limit, timezone_offset,
                shop_id, check_step, is_success
        );
    }

}