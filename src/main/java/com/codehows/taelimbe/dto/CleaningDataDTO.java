package com.codehows.taelimbe.dto;


import com.codehows.taelimbe.entity.CleaningData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 청소 데이터 전송 객체(DTO)입니다.
 * 클라이언트와 서비스 계층 간에 청소 데이터를 주고받을 때 사용됩니다.
 * 엔티티와 분리하여 API 계약을 명확히 하고, 불필요한 데이터 노출을 방지합니다.
 *
 * `@Data`는 Lombok 어노테이션으로, `@Getter`, `@Setter`, `@EqualsAndHashCode`, `@ToString`을 자동으로 생성합니다.
 * `@NoArgsConstructor`는 인자 없는 기본 생성자를 자동으로 생성합니다.
 * `@AllArgsConstructor`는 모든 필드를 인자로 받는 생성자를 자동으로 생성합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleaningDataDTO {

    private Long cleaningId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private Long duration;

    private Double areaCleaned;

    private Double waterUsage;

    private Double powerUsage;

    public static CleaningDataDTO create(CleaningData cleaningData) {
        CleaningDataDTO dto = new CleaningDataDTO();
        dto.setCleaningId(cleaningData.getCleaningId());
        dto.setStartTime(cleaningData.getStartTime());
        dto.setEndTime(cleaningData.getEndTime());
        dto.setLocation(cleaningData.getLocation());
        dto.setDuration(cleaningData.getDuration());
        dto.setAreaCleaned(cleaningData.getAreaCleaned());
        dto.setWaterUsage(cleaningData.getWaterUsage());
        dto.setPowerUsage(cleaningData.getPowerUsage());
        return dto;
    }
}
