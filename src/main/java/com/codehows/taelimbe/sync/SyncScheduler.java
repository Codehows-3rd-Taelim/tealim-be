package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.ReportService;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class SyncScheduler {

    private final StoreService storeService;
    private final RobotService robotService;
    private final ReportService reportService;

    /**
     * 매일 0:00, 3:00, 6:00, 9:00, 12:00, 15:00, 18:00, 21:00에 매장 정보 동기화
     */
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncStoresScheduled() {
        System.out.println("\n[SCHEDULER] Starting Store Sync at " + LocalDateTime.now());
        try {
            int count = storeService.syncAllStores();
            System.out.println("[SCHEDULER] Store Sync Completed: " + count + " stores");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Store Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 매일 0:30, 3:30, 6:30, 9:30, 12:30, 15:30, 18:30, 21:30에 로봇 정보 동기화
     */
    @Scheduled(cron = "0 30 0/3 * * *", zone = "Asia/Seoul")
    public void syncRobotsScheduled() {
        System.out.println("\n[SCHEDULER] Starting Robot Sync at " + LocalDateTime.now());
        try {
            int count = robotService.syncAllStoresRobots();
            System.out.println("[SCHEDULER] Robot Sync Completed: " + count + " robots");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Robot Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 매일 1:00, 4:00, 7:00, 10:00, 13:00, 16:00, 19:00, 22:00에 Report 동기화 (지난 3시간)
     */
    @Scheduled(cron = "0 0 1/3 * * *", zone = "Asia/Seoul")
    public void syncReportsScheduled() {
        System.out.println("\n[SCHEDULER] Starting Report Sync at " + LocalDateTime.now());
        try {
            // 현재 시간 기준 지난 3시간
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(3);

            TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
                    .startTime(startTime)
                    .endTime(endTime)
                    .timezoneOffset(0)
                    .build();

            int count = reportService.syncAllStoresByTimeRange(req);
            System.out.println("[SCHEDULER] Report Sync Completed: " + count + " reports");
        } catch (Exception e) {
            System.out.println("[SCHEDULER] Report Sync Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}