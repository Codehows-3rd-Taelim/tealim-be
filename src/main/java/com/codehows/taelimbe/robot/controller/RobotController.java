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
@RequestMapping("/api/robot")
public class RobotController {

    private final RobotService robotService;

    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@Valid @RequestBody RobotSyncRequestDTO req) {
        int count = robotService.syncRobots(req);
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료");
    }

    @GetMapping("/{sn}")
    public ResponseEntity<RobotDTO> getRobot(@PathVariable String sn) {
        return ResponseEntity.ok(robotService.getRobotBySn(sn));
    }

    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getAllRobots(@RequestParam Long storeId) {
        return ResponseEntity.ok(robotService.getRobotListFromDB(storeId));
    }
}