package com.codehows.taelimbe.sync;

import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
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
    private final PuduReportService puduReportService;

    // 매장 동기화 + 로봇 동기화
    // 시간 : 00:00 / 03:00 / 06:00 / 09:00 / 12:00 / 15:00 / 18:00 / 21:00
    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncStoresAndRobotsScheduled() {
        System.out.println("\n[SCHEDULER] === Store + Robot Sync Start === " + LocalDateTime.now());

        try {
            int storeCount = storeService.syncAllStores();
            System.out.println("[SCHEDULER] Store Sync Completed → " + storeCount + " stores");

            int robotCount = robotService.syncAllStoresRobots();
            System.out.println("[SCHEDULER] Robot Sync Completed → " + robotCount + " robots");

            System.out.println("[SCHEDULER] === Store + Robot Sync FINISHED ===\n");
        } catch (Exception e) {
            System.out.println("[SCHEDULER]  Store+Robot Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }


     // 시간 : 01:00 / 04:00 / 07:00 / 10:00 / 13:00 / 16:00 / 19:00 / 22:00
    @Scheduled(cron = "0 0 1/3 * * *", zone = "Asia/Seoul")
    public void syncReportsScheduled() {
        System.out.println("\n[SCHEDULER] === Report Sync Start === " + LocalDateTime.now());

        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusHours(3);

            int count = puduReportService.syncAllStoresByTimeRange(
                    TimeRangeSyncRequestDTO.builder()
                            .startTime(start)
                            .endTime(end)
                            .timezoneOffset(0)
                            .build()
            );

            System.out.println("[SCHEDULER] Report Sync Completed → " + count + " reports");
            System.out.println("[SCHEDULER] === Report Sync FINISHED ===\n");

        } catch (Exception e) {
            System.out.println("[SCHEDULER]  Report Sync FAILED : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
