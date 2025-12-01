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

    /**
     * 특정 매장의 로봇 동기화
     * 요청한 매장 ID에 해당하는 로봇을 Pudu API에서 조회하여 DB에 저장/업데이트
     * @param req storeId 포함 요청 정보
     * @return 저장된 로봇 개수
     */
    @PostMapping("/sync")
    public ResponseEntity<String> syncRobots(@Valid @RequestBody RobotSyncRequestDTO req) {
        int count = robotService.syncRobots(req);
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료");
    }

    /**
     * 모든 매장의 로봇 동기화
     * DB에 저장된 모든 매장의 로봇을 Pudu API에서 조회하여 한 번에 동기화
     * 관리자가 전체 로봇 정보를 업데이트할 때 사용
     * @return 저장된 전체 로봇 개수
     */
    @PostMapping("/sync-all-stores")
    public ResponseEntity<String> syncAllStoresRobots() {
        int count = robotService.syncAllStoresRobots();
        return ResponseEntity.ok(count + "개 로봇 저장/업데이트 완료 (모든 매장)");
    }

    /**
     * 시리얼 번호로 로봇 조회
     * DB에서 특정 시리얼 번호의 로봇 정보를 조회
     * @param sn 로봇 시리얼 번호
     * @return 로봇 정보
     */
    @GetMapping("/{sn}")
    public ResponseEntity<RobotDTO> getRobot(@PathVariable String sn) {
        return ResponseEntity.ok(robotService.getRobotBySn(sn));
    }

    /**
     * 매장별 로봇 목록 조회
     * 특정 매장에 속한 모든 로봇 목록을 DB에서 조회
     * @param storeId 매장 ID
     * @return 해당 매장의 로봇 목록
     */
    @GetMapping("/list")
    public ResponseEntity<List<RobotDTO>> getAllRobots(@RequestParam Long storeId) {
        return ResponseEntity.ok(robotService.getRobotListFromDB(storeId));
    }
}