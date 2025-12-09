package com.codehows.taelimbe.pudureport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuduReportDetailRequestDTO {

    @NotNull(message = "storeId는 필수입니다")
    @Positive(message = "storeId는 양수여야 합니다")
    private Long storeId;

    @NotBlank(message = "sn은 필수입니다")
    private String sn;

    @NotBlank(message = "reportId는 필수입니다")
    private String reportId;

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