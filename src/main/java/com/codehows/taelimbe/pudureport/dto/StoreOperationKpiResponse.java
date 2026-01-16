package com.codehows.taelimbe.pudureport.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreOperationKpiResponse {

    private Long storeId;
    private String shopName;

    private Float plannedCleanTime; // 분
    private Float actualCleanTime;  // 분
    private Float operationRate;    // %

    private String status; // GOOD / WARN / BAD
}
