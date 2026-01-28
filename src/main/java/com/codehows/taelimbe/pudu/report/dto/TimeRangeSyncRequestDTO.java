package com.codehows.taelimbe.pudu.report.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeRangeSyncRequestDTO {

    @NotNull(message = "startTime은 필수입니다")
    private LocalDateTime startTime;

    @NotNull(message = "endTime은 필수입니다")
    private LocalDateTime endTime;

    @Builder.Default
    private Integer timezoneOffset = 0;

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime.isBefore(endTime);
    }
}