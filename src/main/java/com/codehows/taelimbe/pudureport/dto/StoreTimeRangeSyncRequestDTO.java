package com.codehows.taelimbe.pudureport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

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
    private LocalDateTime startTime;

    @NotNull(message = "endTime은 필수입니다")
    private LocalDateTime endTime;

    @Builder.Default
    private Integer timezoneOffset = 0;

    @Builder.Default
    private Integer offset = 0;

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime.isBefore(endTime);
    }
}