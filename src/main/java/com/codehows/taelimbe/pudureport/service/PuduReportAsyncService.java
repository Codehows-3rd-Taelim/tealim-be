package com.codehows.taelimbe.pudureport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PuduReportAsyncService {

    private final PuduReportService puduReportService;

    @Async("PuduReportSyncExecutor")
    public void syncAllStoresFullHistoricalAsync() {
        puduReportService.syncAllStoresFullHistorical();
    }
}
