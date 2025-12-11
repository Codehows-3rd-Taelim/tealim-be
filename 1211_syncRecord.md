# Store.java

```java
package com.codehows.taelimbe.store.entity;

import com.codehows.taelimbe.store.constant.DeleteStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "store")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "shop_name", length = 20, nullable = false)
    private String shopName;

    @Enumerated(EnumType.STRING)
    @Column(name = "del_yn", nullable = false, length = 1)
    @Builder.Default
    private DeleteStatus delYn = DeleteStatus.N;

    @ManyToOne
    @JoinColumn(name = "industry_id")
    private Industry industry;

}
```

# SyncRecord.java

```java
package com.codehows.taelimbe.sync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * storeId = 0 → ADMIN 전체 동기화 시간
     * storeId > 0 → 해당 매장의 동기화 시간
     */
    @Column(name = "store_id", nullable = false, unique = true)
    private Long storeId;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
}
```

# SyncRecordRepository.java

```java
package com.codehows.taelimbe.sync.repository;

import com.codehows.taelimbe.sync.entity.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    Optional<SyncRecord> findByStoreId(Long storeId);
}
```

# SyncRecordService.java

```java
package com.codehows.taelimbe.sync.service;

import com.codehows.taelimbe.sync.entity.SyncRecord;
import com.codehows.taelimbe.sync.repository.SyncRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SyncRecordService {

    private final SyncRecordRepository syncRecordRepository;

    //  마지막 동기화 시간 저장 (ADMIN / USER / MANAGER 공통)
    public void updateSyncTime(Long storeId, LocalDateTime syncStartTime) {
        SyncRecord record = syncRecordRepository.findByStoreId(storeId)
                .orElseGet(() -> SyncRecord.builder()
                        .storeId(storeId)
                        .build()
                );

        record.setLastSyncTime(syncStartTime);
        syncRecordRepository.save(record);
    }


    // 마지막 동기화 시간 조회 (ADMIN / USER / MANAGER 공통)
    public LocalDateTime getLastSyncTime(Long storeId) {
        return syncRecordRepository.findByStoreId(storeId)
                .map(SyncRecord::getLastSyncTime)
                .orElse(null);
    }

    // 동기화 시간 저장 (ADMIN용) - 수정됨: storeId를 0L로 고정
    public void updateAdminSyncTime(LocalDateTime syncStartTime) {
        updateSyncTime(0L, syncStartTime); // storeId 0L은 ADMIN 전체 동기화를 의미
    }


    // USER/MANAGER용: 해당 매장 동기화 시간 조회
    public LocalDateTime getStoreSyncTime(Long storeId) {
        return getLastSyncTime(storeId);
    }
}
```

# SyncController.java

```java
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
        syncRecordService.updateSyncTime(storeId, syncStartTime);

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
            lastTime = syncRecordService.getLastSyncTime(0L); // storeId 0L은 ADMIN 전체 동기화를 의미
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
```

# SyncScheduler.java

```java
package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.sync.service.SyncRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;
    private final SyncRecordService syncRecordService;

    /**
     * 매장 전체 동기화 + 로봇 전체 동기화
     * (00, 03, 06, 09, 12, 15, 18, 21시)
     */
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncStoresAndRobotsScheduled() {

        LocalDateTime syncStartTime = LocalDateTime.now();
        System.out.println("\n[SCHEDULER] === Store + Robot Sync Start === " + syncStartTime);

        try {
            // 1) 실동기화
            int storeCount = storeService.syncAllStores();
            int robotCount = robotService.syncAllStoresRobots();

            System.out.println("[SCHEDULER] Store Sync Completed → " + storeCount + " stores");
            System.out.println("[SCHEDULER] Robot Sync Completed → " + robotCount + " robots");

            // 2) 모든 매장의 동기화 시간 기록
            List<Store> stores = storeService.findAllStores();
            for (Store store : stores) {
                syncRecordService.updateSyncTime(store.getStoreId(), syncStartTime);
            }

            // 3) 관리자(Admin, storeId=0)의 동기화 시간 기록
            syncRecordService.updateAdminSyncTime(syncStartTime);

            System.out.println("[SCHEDULER] === Store + Robot Sync FINISHED ===\n");

        } catch (Exception e) {
            System.out.println("[SCHEDULER] Store+Robot Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 보고서 전체 동기화
     * (01, 04, 07, 10, 13, 16, 19, 22시)
     */
    @Scheduled(cron = "0 0 1/3 * * *", zone = "Asia/Seoul")
    public void syncReportsScheduled() {

        LocalDateTime syncStartTime = LocalDateTime.now();
        System.out.println("\n[SCHEDULER] === Report Sync Start === " + syncStartTime);

        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusHours(3);

            // 1) 보고서 전체 동기화
            int count = puduReportService.syncAllStoresByTimeRange(
                    TimeRangeSyncRequestDTO.builder()
                            .startTime(start)
                            .endTime(end)
                            .timezoneOffset(0)
                            .build()
            );

            System.out.println("[SCHEDULER] Report Sync Completed → " + count + " reports");

            // 2) 각 매장 동기화 시간 기록
            List<Store> stores = storeService.findAllStores();
            for (Store store : stores) {
                syncRecordService.updateSyncTime(store.getStoreId(), syncStartTime);
            }

            // 3) Admin 전체 시스템 동기화 기록
            syncRecordService.updateAdminSyncTime(syncStartTime);

            System.out.println("[SCHEDULER] === Report Sync FINISHED ===\n");

        } catch (Exception e) {
            System.out.println("[SCHEDULER] Report Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
