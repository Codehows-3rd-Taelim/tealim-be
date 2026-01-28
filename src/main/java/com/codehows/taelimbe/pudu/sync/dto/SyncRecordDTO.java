package com.codehows.taelimbe.pudu.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncRecordDTO {

    private LocalDateTime lastSyncTime;
    private LocalDateTime globalSyncTime;
}
