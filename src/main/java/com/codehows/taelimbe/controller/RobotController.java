package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.dto.RobotDTO;
import com.codehows.taelimbe.service.RobotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/robot")
public class RobotController {

    private final RobotService robotService;

    /**
     * 단일 로봇 조회
     */
    @GetMapping("/info")
    public ResponseEntity<RobotDTO> getRobotInfo(
            @RequestParam String sn,
            @RequestParam Long shopId
    ) {
        return ResponseEntity.ok(robotService.getRobotInfo(sn, shopId));
    }

    /**
     * 매장 전체 로봇 조회
     */
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getRobotList(
            @RequestParam Long shopId
    ) {
        return ResponseEntity.ok(robotService.getRobotListByShop(shopId));
    }

    /**
     * V2 상태 조회
     */
    @GetMapping("/status/v2")
    public ResponseEntity<String> getStatusV2(
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String mac
    ) {
        return robotService.getRobotStatusV2(sn, mac);
    }
}
