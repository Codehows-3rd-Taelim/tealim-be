package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.RobotDTO;
import com.codehows.taelimbe.service.RobotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robot")
public class RobotController {

    private final RobotService robotService;

    // ① SN 기준 로봇 데이터 전체 통합 조회
    @GetMapping("/info")
    public ResponseEntity<RobotDTO> getRobotInfo(
            @RequestParam String sn,
            @RequestParam Long shop_id
    ) {
        return ResponseEntity.ok(robotService.combineRobotInfo(sn, shop_id));
    }

    // ② 로봇 상태 V2 (옵션)
    @GetMapping("/status/v2")
    public ResponseEntity<String> getStatusV2(
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String mac
    ) {
        return robotService.getRobotStatusV2(sn, mac);
    }
}
