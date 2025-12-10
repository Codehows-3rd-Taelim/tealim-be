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
                syncRecordService.updateStoreSyncTime(store.getStoreId(), syncStartTime);
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
                syncRecordService.updateStoreSyncTime(store.getStoreId(), syncStartTime);
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
