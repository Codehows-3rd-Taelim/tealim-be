package com.codehows.taelimbe.report.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportSyncRequestDTO {

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
    private Integer offset = 0;  // 페이징을 위한 offset 추가

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime < endTime;
    }
}