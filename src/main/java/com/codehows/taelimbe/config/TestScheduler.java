//package com.codehows.taelimbe.config;
//
//import com.codehows.taelimbe.report.dto.TimeRangeSyncRequestDTO;
//import com.codehows.taelimbe.report.service.ReportService;
//import com.codehows.taelimbe.robot.service.RobotService;
//import com.codehows.taelimbe.store.service.StoreService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.time.LocalDateTime;
//
//@Component
//@EnableScheduling
//@RequiredArgsConstructor
//public class TestScheduler {
//
//    private final StoreService storeService;
//    private final RobotService robotService;
//    private final ReportService reportService;
//
//    /**
//     * 테스트: 매일 오후 2시 45분에 매장 동기화
//     */
//    @Scheduled(cron = "0 45 14 * * *", zone = "Asia/Seoul")
//    public void syncStoresScheduled() {
//        System.out.println("\n[TEST SCHEDULER] Starting Store Sync at " + LocalDateTime.now());
//        try {
//            int count = storeService.syncAllStores();
//            System.out.println("[TEST SCHEDULER] Store Sync Completed: " + count + " stores\n");
//        } catch (Exception e) {
//            System.out.println("[TEST SCHEDULER] Store Sync Failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 테스트: 매일 오후 2시 50분에 로봇 동기화
//     */
//    @Scheduled(cron = "0 50 14 * * *", zone = "Asia/Seoul")
//    public void syncRobotsScheduled() {
//        System.out.println("\n[TEST SCHEDULER] Starting Robot Sync at " + LocalDateTime.now());
//        try {
//            int count = robotService.syncAllStoresRobots();
//            System.out.println("[TEST SCHEDULER] Robot Sync Completed: " + count + " robots\n");
//        } catch (Exception e) {
//            System.out.println("[TEST SCHEDULER] Robot Sync Failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 테스트: 매일 오후 2시 55분에 Report 동기화 (지난 3시간)
//     */
//    @Scheduled(cron = "0 55 14 * * *", zone = "Asia/Seoul")
//    public void syncReportsScheduled() {
//        System.out.println("\n[TEST SCHEDULER] Starting Report Sync at " + LocalDateTime.now());
//        try {
//            // 현재 시간 기준 지난 3시간
//            long endTime = System.currentTimeMillis() / 1000;
//            long startTime = endTime - (3 * 60 * 60);
//
//            TimeRangeSyncRequestDTO req = TimeRangeSyncRequestDTO.builder()
//                    .startTime(startTime)
//                    .endTime(endTime)
//                    .timezoneOffset(0)
//                    .build();
//
//            int count = reportService.syncAllStoresByTimeRange(req);
//            System.out.println("[TEST SCHEDULER] Report Sync Completed: " + count + " reports\n");
//        } catch (Exception e) {
//            System.out.println("[TEST SCHEDULER] Report Sync Failed: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}