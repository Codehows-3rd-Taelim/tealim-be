package com.codehows.taelimbe.config;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.sync.SyncResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 수동 동기화용
    @PostMapping("/now")
    public ResponseEntity<SyncResultDTO> syncNow() {
        System.out.println("\n[MANUAL SYNC] Starting Full Sync at " + LocalDateTime.now());

        int storeCount = 0;
        int robotCount = 0;
        int reportCount = 0;
        StringBuilder errorMessage = new StringBuilder();

        try {
            // 1. Store 동기화
            System.out.println("[MANUAL SYNC] Starting Store Sync...");
            try {
                storeCount = storeService.syncAllStores();
                System.out.println("[MANUAL SYNC] Store Sync Completed: " + storeCount + " stores");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Store Sync Failed: " + e.getMessage());
                errorMessage.append("Store sync failed: ").append(e.getMessage()).append("\n");
            }

            // 2. Robot 동기화
            System.out.println("[MANUAL SYNC] Starting Robot Sync...");
            try {
                robotCount = robotService.syncAllStoresRobots();
                System.out.println("[MANUAL SYNC] Robot Sync Completed: " + robotCount + " robots");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Robot Sync Failed: " + e.getMessage());
                errorMessage.append("Robot sync failed: ").append(e.getMessage()).append("\n");
            }

            // 3. Report 동기화 (지난 3시간)
            System.out.println("[MANUAL SYNC] Starting Report Sync...");
            try {
                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = endTime.minusHours(3);

                TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
                        .startTime(startTime)
                        .endTime(endTime)
                        .timezoneOffset(0)
                        .build();

                reportCount = puduReportService.syncAllStoresByTimeRange(req);
                System.out.println("[MANUAL SYNC] Report Sync Completed: " + reportCount + " reports");
            } catch (Exception e) {
                System.out.println("[MANUAL SYNC] Report Sync Failed: " + e.getMessage());
                errorMessage.append("Report sync failed: ").append(e.getMessage()).append("\n");
            }

            System.out.println("\n[MANUAL SYNC] Full Sync Completed");
            System.out.println("[MANUAL SYNC] Stores: " + storeCount + ", Robots: " + robotCount + ", Reports: " + reportCount);

            SyncResultDTO result = SyncResultDTO.builder()
                    .storeCount(storeCount)
                    .robotCount(robotCount)
                    .reportCount(reportCount)
                    .totalCount(storeCount + robotCount + reportCount)
                    .success(errorMessage.length() == 0)
                    .errorMessage(errorMessage.toString())
                    .syncTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.out.println("[MANUAL SYNC] Unexpected Error: " + e.getMessage());
            e.printStackTrace();

            SyncResultDTO result = SyncResultDTO.builder()
                    .storeCount(storeCount)
                    .robotCount(robotCount)
                    .reportCount(reportCount)
                    .totalCount(storeCount + robotCount + reportCount)
                    .success(false)
                    .errorMessage("Unexpected error: " + e.getMessage())
                    .syncTime(LocalDateTime.now())
                    .build();

            return ResponseEntity.internalServerError().body(result);
        }
    }
}