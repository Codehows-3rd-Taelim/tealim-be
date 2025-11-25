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

    @GetMapping("/full-info")
    public ResponseEntity<?> getRobotFullInfo(
            @RequestParam String sn,
            @RequestParam long shop_id) {
        return robotRobotService.getRobotFullInfo(sn, shop_id);
    }
}
