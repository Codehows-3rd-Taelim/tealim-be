package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PuduReportFullHistoricalAsyncService {

    private final PuduReportService puduReportService;

    @Async("PuduReportSyncExecutor")
    public CompletableFuture<Integer> syncSingleStore6MonthAsync(Long storeId) {

        LocalDateTime start = LocalDate.now().minusDays(180).atStartOfDay();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);

        int saved = puduReportService.syncSingleStoreByTimeRangeSyncOnly(
                StoreTimeRangeSyncRequestDTO.builder()
                        .storeId(storeId)
                        .startTime(start)
                        .endTime(end)
                        .timezoneOffset(0)
                        .offset(0)
                        .build()
        );

        return CompletableFuture.completedFuture(saved);
    }
}

