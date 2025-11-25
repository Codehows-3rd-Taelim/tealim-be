package com.codehows.taelimbe.service;

import com.codehows.taelimbe.dto.CleaningDataDTO;
import com.codehows.taelimbe.repository.CleaningDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 청소 데이터 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * `@Service` 어노테이션은 이 클래스가 비즈니스 계층의 컴포넌트임을 나타내며,
 * Spring 컨테이너에 의해 관리되는 빈으로 등록됩니다.
 * `@RequiredArgsConstructor`는 Lombok 어노테이션으로, final 필드에 대한 생성자를 자동으로 생성하여 의존성 주입을 용이하게 합니다.
 */
@Service
@RequiredArgsConstructor
public class CleaningDataService {
    // 청소 데이터에 대한 데이터베이스 접근을 담당하는 JpaRepository 인터페이스.
    // Spring Data JPA에 의해 자동으로 구현체가 생성되어 주입됩니다.
    private final CleaningDataRepository repository;

    /**
     * 지정된 시작일과 종료일 사이의 청소 데이터를 페이징하여 조회합니다.
     * 시작일 또는 종료일이 제공되지 않으면 기본값을 사용합니다.
     *
     * @param startDate 조회 시작 날짜 (YYYY-MM-DD 형식의 문자열). null 또는 비어있으면 현재 날짜로부터 1주일 전으로 설정됩니다.
     * @param endDate   조회 종료 날짜 (YYYY-MM-DD 형식의 문자열). null 또는 비어있으면 현재 날짜로 설정됩니다.
     * @return 해당 기간 내의 `CleaningDataDTO` 페이지
     */
    public List<CleaningDataDTO> getCleaningReport(String startDate, String endDate) {
        // 시작일이 null이거나 비어있는 경우, 현재 날짜로부터 1주일 전으로 기본값을 설정합니다.
        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusWeeks(1).toString();
        }
        // 종료일이 null이거나 비어있는 경우, 현재 날짜로 기본값을 설정합니다.
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        // 문자열 형태의 시작일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 시작 시간(00:00:00)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        // 문자열 형태의 종료일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 마지막 시간(23:59:59.999999999)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        // 조회된 데이터 페이지를 DTO 페이지로 변환하여 반환합니다.
        return repository.findByStartTimeBetween(startDateTime, endDateTime)
                .stream()
                .map(CleaningDataDTO::create)
                .toList();
    }
}
