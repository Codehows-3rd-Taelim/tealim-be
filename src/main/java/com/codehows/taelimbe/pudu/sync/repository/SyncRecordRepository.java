package com.codehows.taelimbe.pudu.sync.repository;

import com.codehows.taelimbe.pudu.sync.entity.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    Optional<SyncRecord> findByStore_StoreId(Long storeId);
}
