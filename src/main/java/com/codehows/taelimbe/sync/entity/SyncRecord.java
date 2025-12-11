package com.codehows.taelimbe.sync.entity;

import com.codehows.taelimbe.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_record")
@Getter              // ❗ Getter만 허용
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class SyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    private LocalDateTime lastSyncTime;

    private LocalDateTime globalSyncTime;



    public void updateLastSyncTime(LocalDateTime time) {
        this.lastSyncTime = time;
    }


    public void updateGlobalSyncTime(LocalDateTime time) {
        this.globalSyncTime = time;
    }

    public static SyncRecord create(Store store) {
        return SyncRecord.builder()
                .store(store)
                .build();
    }
}
