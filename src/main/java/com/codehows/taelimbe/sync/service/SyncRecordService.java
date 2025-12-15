package com.codehows.taelimbe.sync.service;

import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import com.codehows.taelimbe.robot.dto.RobotSyncRequestDTO;
import com.codehows.taelimbe.robot.service.RobotService;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.store.service.StoreService;
import com.codehows.taelimbe.sync.dto.SyncRecordDTO;
import com.codehows.taelimbe.sync.entity.SyncRecord;
import com.codehows.taelimbe.sync.repository.SyncRecordRepository;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncRecordService {

    private final SyncRecordRepository syncRecordRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final RobotService robotService;
    private final PuduReportService puduReportService;
    private final UserRepository userRepository;

    /** 현재 로그인 유저 조회 */
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null)
            throw new RuntimeException("Unauthenticated");

        Long userId = (Long) auth.getDetails();
        return userRepository.findById(userId).orElseThrow();
    }

    /** SyncRecord 조회 or 생성 */
    private SyncRecord getOrCreate(Long storeId) {
        return syncRecordRepository.findByStore_StoreId(storeId)
                .orElseGet(() -> {
                    Store store = storeRepository.getReferenceById(storeId);
                    return SyncRecord.create(store);
                });
    }

    /** 일반 매장 sync timestamp */
    public void updateStoreSyncTime(Long storeId, LocalDateTime syncTime) {
        SyncRecord record = getOrCreate(storeId);
        record.updateLastSyncTime(syncTime);
        syncRecordRepository.save(record);
    }

    /** 전체 sync timestamp */
    public void updateGlobalSyncTime(Long storeId, LocalDateTime syncTime) {
        SyncRecord record = getOrCreate(storeId);
        record.updateGlobalSyncTime(syncTime);
        syncRecordRepository.save(record);
    }

    /** 버튼 눌렀을 때 실행되는 공통 sync */
    public String executeSync(Long userId) {

        // userId 기반으로 유저 조회
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Unauthenticated"));

        var role = user.getRole();
        var storeId = user.getStore().getStoreId();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(3);
        LocalDateTime syncStart = LocalDateTime.now();

        TimeRangeSyncRequestDTO syncReq = TimeRangeSyncRequestDTO.builder()
                .startTime(start)
                .endTime(end)
                .timezoneOffset(0)
                .build();

        if (role.getLevel() >= Role.ADMIN.getLevel()) {
            return executeAdminSync(syncStart, syncReq);
        }

        return executeStoreSync(storeId, syncStart, syncReq);
    }

    /** 관리자 전체 sync */
    private String executeAdminSync(LocalDateTime syncStart, TimeRangeSyncRequestDTO syncReq) {
        int storeCnt = storeService.syncAllStores();
        int robotCnt = robotService.syncAllStoresRobots();
        int reportCnt = puduReportService.syncAllStoresByTimeRange(syncReq);

        List<Store> stores = storeService.findAllStores();
        for (Store s : stores) {
            Long sid = s.getStoreId();
            updateStoreSyncTime(sid, syncStart);
            updateGlobalSyncTime(sid, syncStart);
        }

        return "Store:" + storeCnt + "개 /" + " Robot:" + robotCnt + "개 /" + " Report:" + reportCnt + "개 추가 되었습니다!";
    }

    /** 단일 매장 sync */
    private String executeStoreSync(Long storeId, LocalDateTime syncStart, TimeRangeSyncRequestDTO syncReq) {

        int robotCnt = robotService.syncRobots(new RobotSyncRequestDTO(storeId));

        // 리포트만 기존 DTO 사용
        StoreTimeRangeSyncRequestDTO reportReq = StoreTimeRangeSyncRequestDTO.builder()
                .storeId(storeId)
                .startTime(syncReq.getStartTime())
                .endTime(syncReq.getEndTime())
                .timezoneOffset(syncReq.getTimezoneOffset())
                .build();

        int reportCnt = puduReportService.syncSingleStoreByTimeRange(reportReq);

        updateStoreSyncTime(storeId, syncStart);

        return "Robot:" + robotCnt + "개 /" + " Report:" + reportCnt + "개 " + "추가 되었습니다!";
    }

    /** 프론트 조회용 DTO */
    public SyncRecordDTO getLastSyncTime(Long userId) {

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Unauthenticated"));

        Long storeId = user.getStore().getStoreId();

        SyncRecord record = syncRecordRepository.findByStore_StoreId(storeId)
                .orElse(null);

        if (record == null) return new SyncRecordDTO();

        return SyncRecordDTO.builder()
                .lastSyncTime(record.getLastSyncTime())
                .globalSyncTime(record.getGlobalSyncTime())
                .build();
    }
}
