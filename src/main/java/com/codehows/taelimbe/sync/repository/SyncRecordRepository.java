package com.codehows.taelimbe.sync.repository;

import com.codehows.taelimbe.sync.entity.SyncRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    Optional<SyncRecord> findByStoreId(Long storeId);
}
