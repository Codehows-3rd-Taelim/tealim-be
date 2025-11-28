package com.codehows.taelimbe.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 'cleaning_data' 테이블과 매핑되는 JPA 엔티티 클래스입니다.
 * 청소 작업 데이터를 나타냅니다.
 *
 * `@Entity`는 이 클래스가 JPA 엔티티임을 나타냅니다.
 * `@Data`는 Lombok 어노테이션으로, `@Getter`, `@Setter`, `@EqualsAndHashCode`, `@ToString`을 자동으로 생성합니다.
 */
@Entity
@Data
public class CleaningData {

    /**
     * 엔티티의 기본 키(Primary Key)입니다.
     * `@Id`는 이 필드가 엔티티의 식별자임을 나타냅니다.
     * `@GeneratedValue(strategy = GenerationType.IDENTITY)`는 기본 키 생성을 데이터베이스에 위임합니다 (예: MySQL의 AUTO_INCREMENT).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cleaningId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private Long duration; // in minutes

    private Double areaCleaned; // in square meters

    private Double waterUsage; // in liters

    private Double powerUsage; // in kWh
}
