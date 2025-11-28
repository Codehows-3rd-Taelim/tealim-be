package com.codehows.taelimbe.service;

import com.codehows.taelimbe.dto.ReportDTO;
import com.codehows.taelimbe.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public List<ReportDTO> getReport(String startDate, String endDate) {

        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusWeeks(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        // 문자열 형태의 시작일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 시작 시간(00:00:00)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        // 문자열 형태의 종료일을 `LocalDate` 객체로 파싱하고, 해당 날짜의 마지막 시간(23:59:59.999999999)으로 `LocalDateTime`을 생성합니다.
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        return reportRepository.findByReportDateBetween(startDateTime, endDateTime)
                .stream()
                .map(ReportDTO::createReportDTO)
                .toList();
    }
}
