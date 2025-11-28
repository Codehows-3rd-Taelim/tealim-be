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

    // 1) 전체 Robot 동기화(API → DB)
    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@RequestParam Long storeId) {
        int count = robotService.syncRobotsByStoreId(storeId);
        return ResponseEntity.ok(count + "개 Robot 저장/업데이트 완료");
    }

    // 2) 단일 조회(DB + API 최신값 병합)
    @GetMapping("/info")
    public ResponseEntity<RobotDTO> getRobotInfo(
            @RequestParam Long storeId,
            @RequestParam String sn
    ) {
        return ResponseEntity.ok(robotService.getRobotInfoByStoreId(sn, storeId));
    }

    // 3) 전체 Robot 조회(DB 전용)
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getRobotList(
            @RequestParam Long storeId
    ) {
        return ResponseEntity.ok(robotService.getRobotListFromDB(storeId));
    }


}
