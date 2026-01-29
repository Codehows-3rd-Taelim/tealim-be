package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PuduReportFullHistoricalFacade {

    private final StoreRepository storeRepository;
    private final PuduReportFullHistoricalAsyncService asyncService;

    public void syncAllStores6MonthAsync() {

        List<Long> storeIds = storeRepository.findAll()
                .stream()
                .map(Store::getStoreId)
                .toList();

        List<CompletableFuture<Integer>> futures = storeIds.stream()
                .map(asyncService::syncSingleStore6MonthAsync)
                .toList();

        // 백그라운드에서 끝까지 돌게만 함
        CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        ).whenComplete((v, e) -> {
            if (e != null) {
                System.err.println(" 전체 매장 6개월 동기화 실패");
                e.printStackTrace();
            } else {
                int total = futures.stream()
                        .mapToInt(CompletableFuture::join)
                        .sum();
                System.out.println("전체 매장 6개월 동기화 완료, 저장 수 = " + total);
            }
        });
    }
}
