package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.dto.PuduReportResponseDTO;
import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PuduReportService {

    private final PuduReportAsyncProcessor processor;
    private final PuduReportRepository puduReportRepository;
    private final StoreRepository storeRepository;
    private final RobotRepository robotRepository;

    // 단일 매장 특정 기간 보고서 조회
    @Transactional
    public int syncSingleStoreByTimeRange(StoreTimeRangeSyncRequestDTO req) {

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        int offset = req.getOffset(), saved = 0;
        List<PuduReport> buffer = new ArrayList<>();

        while (true) {

            List<JsonNode> list = processor.fetchList(
                    req.getStartTime(), req.getEndTime(),
                    shopId, req.getTimezoneOffset(), offset);

            if (list.isEmpty()) break;

            List<CompletableFuture<PuduReport>> future = list.stream()
                    .map(x -> processor.convertAsync(
                            x.path("sn").asText(),
                            x.path("report_id").asText(),
                            req.getStartTime(), req.getEndTime(),
                            req.getTimezoneOffset(), shopId))
                    .toList();

            CompletableFuture.allOf(future.toArray(new CompletableFuture[0])).join();

            future.forEach(f -> {
                PuduReport r = f.join();
                System.out.println(">>> REPORT: " + r);
                if (r != null) buffer.add(r);
            });

            if (buffer.size() >= 50) {
                puduReportRepository.saveAll(buffer);
                saved += buffer.size();
                buffer.clear();
            }

            offset += 20;
        }

        if (!buffer.isEmpty()) {
            puduReportRepository.saveAll(buffer);
            saved += buffer.size();
        }

        return saved;
    }


    // 전체 매장 특정 기간 보고서 조회
    @Transactional
    public int syncAllStoresByTimeRange(TimeRangeSyncRequestDTO req){
        return storeRepository.findAll().stream()
                .mapToInt(s -> syncSingleStoreByTimeRange(
                        StoreTimeRangeSyncRequestDTO.builder()
                                .storeId(s.getStoreId())
                                .startTime(req.getStartTime())
                                .endTime(req.getEndTime())
                                .timezoneOffset(req.getTimezoneOffset())
                                .offset(0)
                                .build()
                )).sum();
    }

    // 전체 매장 최대기간(6개월) 보고서 조회
    @Transactional
    public int syncAllStoresFullHistorical(){
        LocalDateTime start = LocalDate.now().minusDays(180).atStartOfDay();
        LocalDateTime end   = LocalDate.now().atTime(LocalTime.MAX);

        return storeRepository.findAll().stream()
                .mapToInt(s -> syncSingleStoreByTimeRange(
                        StoreTimeRangeSyncRequestDTO.builder()
                                .storeId(s.getStoreId())
                                .startTime(start)
                                .endTime(end)
                                .timezoneOffset(0)
                                .offset(0)
                                .build()))
                .sum();
    }

    // db에서 상세 보고서 목록 가져오기
    public List<PuduReportDTO> getAllReports(){
        return puduReportRepository.findAll().stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }

    // id로 보고서 가져오기
    public PuduReportDTO getReportById(Long id){
        return puduReportRepository.findById(id)
                .map(PuduReportDTO::createReportDTO)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: "+id));
    }

    // sn으로 상세 보고서 목록 가져오기
    public List<PuduReportDTO> getReportsByRobotSn(String sn){
        return puduReportRepository.findByRobot_Sn(sn).stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }

    public List<PuduReportDTO> getReport(String startDate, String endDate){

        if(startDate == null || startDate.isEmpty())
            startDate = LocalDate.now().minusWeeks(1).toString();

        if(endDate == null || endDate.isEmpty())
            endDate = LocalDate.now().toString();

        LocalDateTime s = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime e = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        return puduReportRepository.findByStartTimeBetween(s,e).stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }

    @Transactional
    public List<PuduReportResponseDTO> getReports(Long storeId, String startDate, String endDate) {
        List<PuduReport> puduReports;

        // 날짜 범위 설정
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        // DateTimeFormatter 정의
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        if (startDate != null && !startDate.isEmpty()) {
            try {
                startDateTime = LocalDateTime.parse(startDate, formatter);
                System.out.println("시작 시간: " + startDateTime);
            } catch (DateTimeParseException e) {
                System.err.println("시작 날짜 파싱 오류: " + startDate);
                e.printStackTrace();
            }
        }

        if (endDate != null && !endDate.isEmpty()) {
            try {
                endDateTime = LocalDateTime.parse(endDate, formatter);
                System.out.println("종료 시간: " + endDateTime);
            } catch (DateTimeParseException e) {
                System.err.println("종료 날짜 파싱 오류: " + endDate);
                e.printStackTrace();
            }
        }

        if (storeId != null) {
            List<Robot> robots = robotRepository.findAllByStore_StoreId(storeId);

            if (robots.isEmpty()) {
                System.out.println("매장 ID " + storeId + "에 등록된 로봇이 없습니다.");
                return List.of();
            }

            List<Long> robotIds = robots.stream()
                    .map(Robot::getRobotId)
                    .toList();

            if (startDateTime != null && endDateTime != null) {
                puduReports = puduReportRepository.findAllByRobot_RobotIdInAndStartTimeBetween(
                        robotIds, startDateTime, endDateTime);
                System.out.println("매장 + 날짜 필터링 조회: " + puduReports.size() + "개");
            } else {
                puduReports = puduReportRepository.findAllByRobot_RobotIdIn(robotIds);
                System.out.println("매장 필터링만 조회: " + puduReports.size() + "개");
            }

        } else {
            if (startDateTime != null && endDateTime != null) {
                puduReports = puduReportRepository.findByStartTimeBetween(startDateTime, endDateTime);
                System.out.println("날짜 필터링만 조회: " + puduReports.size() + "개");
            } else {
                puduReports = puduReportRepository.findAll();
                System.out.println("전체 조회: " + puduReports.size() + "개");
            }
        }

        return puduReports.stream()
                .map(PuduReportResponseDTO::createReportResponseDTO)
                .toList();
    }

}
