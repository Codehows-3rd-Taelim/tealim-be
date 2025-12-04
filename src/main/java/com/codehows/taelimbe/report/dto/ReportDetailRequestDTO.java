package com.codehows.taelimbe.report.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Report 상세 정보 저장 요청 DTO
 *
 * 용도:
 * - 특정 Report의 상세 정보를 Pudu API에서 조회하여 DB에 저장
 *
 * 사용 예시:
 * POST /api/report/detail/save
 * {
 *   "storeId": 1,
 *   "sn": "PUDU123456",
 *   "reportId": "987654321",
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
public class ReportDetailRequestDTO {

    @NotNull(message = "storeId는 필수입니다")
    @Positive(message = "storeId는 양수여야 합니다")
    private Long storeId;

    @NotBlank(message = "sn은 필수입니다")
    private String sn;

    @NotBlank(message = "reportId는 필수입니다")
    private String reportId;

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