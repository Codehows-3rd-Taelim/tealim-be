package com.codehows.taelimbe.sync.controller;

import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.sync.service.SyncRecordService;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync")
public class SyncController {

    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;
    private final UserRepository userRepository;
    private final SyncRecordService syncRecordService;

    // 동기화 실행 (공통 버튼)
    // ADMIN: 전체 매장 동기화 + storeId=0 기록
    // USER/MANAGER: 자기 매장만 동기화 + 자신의 storeId 기록
    @PostMapping("/now")
    public ResponseEntity<String> sync(@RequestBody(required = false) StoreTimeRangeSyncRequestDTO req) {

        LocalDateTime syncStartTime = LocalDateTime.now();

        // 1)  SecurityContext 에서 userId(PK) 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new RuntimeException("Unauthenticated");
        }

        Long userId = (Long) auth.getDetails(); // User 테이블 PK

        // userId로 DB 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));


        Role role = user.getRole();


        // DEFAULT 요청 Body 없으면 초기화
        if(req == null) req = new StoreTimeRangeSyncRequestDTO();
        req.setTimezoneOffset(0);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        req.setStartTime(start);
        req.setEndTime(end);

        String message;



        // ADMIN 동기화 - 전체 매장
        if (isAdmin(role)) {

            int store = storeService.syncAllStores();
            int robot = robotService.syncAllStoresRobots();

            TimeRangeSyncRequestDTO timeReq = TimeRangeSyncRequestDTO.builder()
                    .startTime(start)
                    .endTime(end)
                    .timezoneOffset(req.getTimezoneOffset())
                    .build();

            int report = puduReportService.syncAllStoresByTimeRange(timeReq);

            //  ADMIN 동기화 시간 기록 (storeId = 0)
            syncRecordService.updateAdminSyncTime(syncStartTime);

            message = "[ADMIN] 동기화 완료 → " +
                    "Store: " + store +
                    " / Robot: " + robot +
                    " / Report: " + report + "개 추가되었습니다";

            return ResponseEntity.ok(message);
        }


            // MANAGER / USER 동기화
        Long storeId = user.getStore().getStoreId();
        req.setStoreId(storeId);

        int robot = robotService.syncRobots(new RobotSyncRequestDTO(storeId));
        int report = puduReportService.syncSingleStoreByTimeRange(req);

        //  해당 매장 동기화 시간 기록 (storeId > 0)
        syncRecordService.updateStoreSyncTime(storeId, syncStartTime);

        message = "[USER/MANAGER] 동기화 완료 → " +
                "매장: " + storeId +
                " / Robot: " + robot +
                " / Report: " + report + "개 추가되었습니다";

        return ResponseEntity.ok(message);
    }

    @GetMapping("/last")
    public ResponseEntity<LocalDateTime> getLastSyncTime() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            throw new RuntimeException("Unauthenticated");
        }

        Long userId = (Long) auth.getDetails(); // User 테이블 PK

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        Role role = user.getRole();
        LocalDateTime lastTime;

        if (isAdmin(role)) {
            // ADMIN → 전체 동기화 시간 (storeId = 0)
            lastTime = syncRecordService.getAdminLastSyncTime();
        } else {
            // USER/MANAGER → 본인 매장 기준 (storeId > 0)
            Long storeId = user.getStore().getStoreId();
            lastTime = syncRecordService.getStoreSyncTime(storeId);
        }

        // lastTime이 null이면 프론트에서 "동기화 기록 없음"으로 처리
        return ResponseEntity.ok(lastTime);
    }

    private boolean isAdmin(Role role) {
        return role.getLevel() >= Role.ADMIN.getLevel();
    }

}
