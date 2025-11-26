package com.codehows.taelimbe.controller;

import com.codehows.taelimbe.service.RobotGeneralService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/robot")
public class RobotGeneralController {

    @Autowired
    private RobotGeneralService robotGeneralService;

    /**
     * 로봇 상태 조회 (V2)
     * 로봇의 SN 또는 MAC을 기반으로 상태 정보 조회
     *
     * @param sn 로봇 SN (선택)
     * @param mac 로봇 MAC (선택) - sn과 mac 중 하나는 필수
     * @return 로봇 상태 정보
     */
    @GetMapping("/status/v2")
    public ResponseEntity<String> getRobotStatusV2(
            @RequestParam(required = false) String sn,
            @RequestParam(required = false) String mac) {
        return robotGeneralService.getRobotStatusV2(sn, mac);
    }
}