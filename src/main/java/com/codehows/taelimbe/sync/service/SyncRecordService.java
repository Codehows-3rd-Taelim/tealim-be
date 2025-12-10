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

    private static final Long ADMIN_STORE_ID = 0L;


     // 동기화 시간 저장 (ADMIN / USER / MANAGER 공통)
     // storeId = 0 → ADMIN 전체 동기화 시간
     // storeId > 0 → 매장별 동기화 시간
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

    // ADMIN용: 전체 동기화 시간 저장
    public void updateAdminSyncTime(LocalDateTime syncStartTime) {
        updateSyncTime(ADMIN_STORE_ID, syncStartTime);
    }


    // ADMIN용: 전체 동기화 시간 조회
    public LocalDateTime getAdminLastSyncTime() {
        return getLastSyncTime(ADMIN_STORE_ID);
    }


    // USER/MANAGER용: 해당 매장 동기화 시간 저장
    public void updateStoreSyncTime(Long storeId, LocalDateTime syncStartTime) {
        updateSyncTime(storeId, syncStartTime);
    }

    // USER/MANAGER용: 해당 매장 동기화 시간 조회
    public LocalDateTime getStoreSyncTime(Long storeId) {
        return getLastSyncTime(storeId);
    }
}