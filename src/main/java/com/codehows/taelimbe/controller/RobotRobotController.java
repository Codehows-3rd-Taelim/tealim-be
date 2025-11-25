package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.RobotRobotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/robot")
public class RobotRobotController {

    @Autowired
    private RobotRobotService robotRobotService;

    @GetMapping("/detail")
    public ResponseEntity<String> getRobotDetail(@RequestParam String sn) {
        return robotRobotService.getRobotDetail(sn);
    }

    @GetMapping("/robot/full-info")
    public ResponseEntity<?> getRobotFullInfo(
            @RequestParam String sn,
            @RequestParam long shop_id,
            @RequestParam(required = false) Long start_time,
            @RequestParam(required = false) Long end_time,
            @RequestParam(defaultValue = "0") int timezone_offset,
            @RequestParam(defaultValue = "10") int limit) {

        if (start_time == null || end_time == null) {
            // 기본값 사용 (24시간 전)
            return robotRobotService.getRobotFullInfo(sn, shop_id);
        } else {
            // 명시적 시간 범위 사용
            return robotRobotService.getRobotFullInfo(sn, shop_id, start_time, end_time, timezone_offset, limit
            );
        }
    }
}
