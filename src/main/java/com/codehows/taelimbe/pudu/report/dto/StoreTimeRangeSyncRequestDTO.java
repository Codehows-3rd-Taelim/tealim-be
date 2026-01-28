package com.codehows.taelimbe.pudu.report.dto;

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

}