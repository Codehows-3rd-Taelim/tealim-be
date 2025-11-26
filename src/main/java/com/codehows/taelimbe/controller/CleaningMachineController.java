package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.CleaningMachineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/robot")
public class CleaningMachineController {

    @Autowired
    private CleaningMachineService cleaningMachineService;

    @GetMapping("/detail")
    public ResponseEntity<String> getRobotDetail(@RequestParam String sn) {
        return cleaningMachineService.getRobotDetail(sn);
    }

    @GetMapping("/full-info")
    public ResponseEntity<?> getRobotFullInfo(
            @RequestParam String sn,
            @RequestParam long shop_id,
            @RequestParam(required = false) Long start_time,
            @RequestParam(required = false) Long end_time,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(defaultValue = "20") int limit) {

        // Service에 6개 파라미터 모두 전달
        // null 값은 Service에서 처리 (기본값 설정)
        return cleaningMachineService.getRobotFullInfo(sn, shop_id, start_time, end_time, timezone_offset, limit);
    }
}