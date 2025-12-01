package com.codehows.taelimbe.report.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * 특정 매장의 특정 기간 Report 동기화 요청 DTO
 *
 * 용도:
 * - 특정 매장의 어제/지난주/특정 기간 데이터 동기화
 *
 * 사용 예시:
 * POST /api/report/sync/store/time-range
 * {
 *   "storeId": 1,
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
public class StoreTimeRangeSyncRequestDTO {

    @NotNull(message = "storeId는 필수입니다")
    @Positive(message = "storeId는 양수여야 합니다")
    private Long storeId;

    @NotNull(message = "startTime은 필수입니다")
    @Positive(message = "startTime은 양수여야 합니다")
    private Long startTime;

    @NotNull(message = "endTime은 필수입니다")
    @Positive(message = "endTime은 양수여야 합니다")
    private Long endTime;

    @Builder.Default
    private Integer timezoneOffset = 0;

    @Builder.Default
    private Integer offset = 0;  // 페이징을 위한 offset (내부용)

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime < endTime;
    }
}