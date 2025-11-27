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
     * 단일 로봇 조회 (storeId → shopId 자동 변환)
     */
    @GetMapping("/info")
    public ResponseEntity<RobotDTO> getRobotInfo(
            @RequestParam Long storeId,
            @RequestParam String sn
    ) {
        return ResponseEntity.ok(robotService.getRobotInfoByStoreId(sn, storeId));
    }

    /**
     * 매장 전체 로봇 조회 (storeId 기반)
     */
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getRobotList(
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(robotService.getRobotListByStoreId(storeId));
    }

    /**
     * 로봇 전체 저장/업데이트 (storeId 기준)
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@RequestParam Long storeId) {
        int count = robotService.syncRobotsByStoreId(storeId);
        return ResponseEntity.ok(count + "대 로봇 저장/업데이트 완료");
    }

    /**
     * V2 상태 조회 (SN or MAC)
     */
    @GetMapping("/status/v2")
    public ResponseEntity<String> getStatusV2(
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String mac
    ) {
        return robotService.getRobotStatusV2(sn, mac);
    }
}
