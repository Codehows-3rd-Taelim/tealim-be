package com.codehows.taelimbe.sync.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_record")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * storeId = 0 → ADMIN 전체 동기화 시간
     * storeId > 0 → 해당 매장의 동기화 시간
     */
    @Column(name = "store_id", nullable = false, unique = true)
    private Long storeId;

    @Column(name = "last_sync_time")
    private LocalDateTime lastSyncTime;
}