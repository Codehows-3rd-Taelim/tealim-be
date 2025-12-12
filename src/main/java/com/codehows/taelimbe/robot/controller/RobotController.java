package com.codehows.taelimbe.robot.controller;

import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
import com.codehows.taelimbe.robot.dto.RobotDTO;
import com.codehows.taelimbe.robot.service.RobotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/robot")
public class RobotController {

    private final RobotService robotService;


    // 특정 매장의 로봇 동기화
    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@Valid @RequestBody RobotSyncRequestDTO req) {
        int count = robotService.syncRobots(req);
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료");
    }


    // 모든 매장의 로봇 동기화
    @PostMapping("/sync-all-stores")
    public ResponseEntity<String> syncAllStoresRobots() {
        int count = robotService.syncAllStoresRobots();
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료 (모든 매장)");
    }


    // 시리얼 번호로 로봇 조회
    @GetMapping("/{sn}")
    public ResponseEntity<RobotDTO> getRobot(@PathVariable String sn) {
        return ResponseEntity.ok(robotService.getRobotBySn(sn));
    }


    // 매장별 로봇 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getAllRobots(@RequestParam Long storeId) {
        return ResponseEntity.ok(robotService.getRobotListFromDB(storeId));
    }


}
