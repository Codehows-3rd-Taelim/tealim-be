package com.codehows.taelimbe.report.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 전체 매장의 특정 기간 Report 동기화 요청 DTO
 *
 * 용도:
 * - 모든 매장의 어제/지난주/특정 기간 데이터 일괄 동기화
 * - 장애 복구 후 특정 시간대 재동기화
 *
 * 특징:
 * - storeId 없음 (전체 매장 대상)
 * - 시간 범위만 지정
 *
 * 사용 예시:
 * POST /api/report/sync/all-stores/time-range
 * {
 *   "startTime": 1733011200,
 *   "endTime": 1733097599,
 *   "timezoneOffset": 0
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeRangeSyncRequestDTO {

    @NotNull(message = "startTime은 필수입니다")
    @Positive(message = "startTime은 양수여야 합니다")
    private Long startTime;

    @NotNull(message = "endTime은 필수입니다")
    @Positive(message = "endTime은 양수여야 합니다")
    private Long endTime;

    @Builder.Default
    private Integer timezoneOffset = 0;

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime < endTime;
    }
}