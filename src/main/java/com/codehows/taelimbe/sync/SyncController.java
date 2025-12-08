package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sync")
public class SyncController {

    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    @PostMapping("/run")
    public ResponseEntity<String> sync(HttpServletRequest request,
                                       @RequestBody(required = false) StoreTimeRangeSyncRequestDTO req) {

        // 1) 토큰에서 유저 정보 추출
        String loginId = jwtService.parseToken(request);
        User user = userRepository.findById(loginId)
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        Role role = user.getRole(); // USER / MANAGER / ADMIN

        // DEFAULT 요청 Body 없으면 초기화
        if(req == null) req = new StoreTimeRangeSyncRequestDTO();
        req.setTimezoneOffset(0);

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        req.setStartTime(start);
        req.setEndTime(end);


        // admin
        if(role.getLevel() >= Role.ADMIN.getLevel()) {

            int store = storeService.syncAllStores();
            int robot = robotService.syncAllStoresRobots();
            int report = puduReportService.syncAllStoresByTimeRange(req);

            return ResponseEntity.ok("[ADMIN] 전체동기화 완료 → " +
                    "Store:"+store+" / Robot:"+robot+" / Report:"+report);
        }


        // manager, employee
        Long storeId = user.getStore().getStoreId();
        req.setStoreId(storeId);

        int robot = robotService.syncRobots(new RobotSyncRequestDTO(storeId));
        int report = puduReportService.syncSingleStoreByTimeRange(req);

        return ResponseEntity.ok("[USER/MANAGER] 매장:" + storeId + " Sync 완료 → " +
                "Robot:"+robot+" / Report:"+report);
    }
}
