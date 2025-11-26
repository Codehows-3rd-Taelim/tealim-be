//package com.codehows.taelimbe.controller;
//
//import com.codehows.taelimbe.service.VRobotGeneralService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//
////이거 key가 권한 없다고 안된다고 함 v2인데
//@RestController
//@RequestMapping("/api/robot")
//public class VRobotGeneralController {
//
//    @Autowired
//    private VRobotGeneralService VRobotGeneralService;
//
//    /**
//     * 로봇 상태 조회 (V2)
//     * 로봇 SN 또는 MAC 기반 상태 조회
//     */
//    @GetMapping("/status/v2")
//    public ResponseEntity<String> getRobotStatusV2(
//            @RequestParam(required = false) String sn,
//            @RequestParam(required = false) String mac) {
//
//        return VRobotGeneralService.getRobotStatusV2(sn, mac);
//    }
//}
