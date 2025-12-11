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

    @Scheduled(cron = "0 0 0/3 * * *", zone = "Asia/Seoul")
    public void syncScheduler() {

        LocalDateTime syncTime = LocalDateTime.now();

        // 1) 매장 전체
        storeService.syncAllStores();

        // 2) 로봇 전체
        robotService.syncAllStoresRobots();

        // 3) 보고서 전체
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);

        TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
                .startTime(start)
                .endTime(end)
                .timezoneOffset(0)
                .build();

        puduReportService.syncAllStoresByTimeRange(req);

        // 4) SyncRecord 업데이트
        List<Store> stores = storeService.findAllStores();
        for (Store s : stores) {
            Long sid = s.getStoreId();
            syncRecordService.updateStoreSyncTime(sid, syncTime);
            syncRecordService.updateGlobalSyncTime(sid, syncTime);
        }

    }
}

