package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.dto.PuduReportResponseDTO;
import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.constant.DeleteStatus;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.codehows.taelimbe.user.constant.Role;
import com.codehows.taelimbe.user.entity.User;
import com.codehows.taelimbe.user.repository.UserRepository;
import com.codehows.taelimbe.user.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PuduReportService {

    private final PuduReportAsyncProcessor processor;
    private final PuduReportRepository puduReportRepository;
    private final StoreRepository storeRepository;
    private final RobotRepository robotRepository;
    private final UserRepository userRepository;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private String resolveSortKey(String sortKey) {
        return switch (sortKey) {
            case "sn" -> "robot.sn";
            case "storeName" -> "robot.store.shopName";
            case "mapName" -> "mapName";
            default -> "startTime";
        };
    }

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

    // id로 보고서 가져오기
    public PuduReportDTO getReportById(Long id){
        return puduReportRepository.findById(id)
                .map(PuduReportDTO::createReportDTO)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: "+id));
    }

    // 특이사항 저장
    @Transactional
    public PuduReportDTO updateRemark(Long puduReportId, String remark) {

        PuduReport report = puduReportRepository.findById(puduReportId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Report not found: " + puduReportId)
                );

        // 특이사항 업데이트
        report.updateRemark(remark);

        // save는 선택이지만 명시적으로 해주는 게 좋음
        puduReportRepository.save(report);

        return PuduReportDTO.createReportDTO(report);
    }

    public List<PuduReportDTO> getReport(String startDate, String endDate){

        if(startDate == null || startDate.isEmpty())
            startDate = LocalDate.now().minusWeeks(1).toString();

        if(endDate == null || endDate.isEmpty())
            endDate = LocalDate.now().toString();

        LocalDateTime s = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime e = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        return puduReportRepository.findByStartTimeBetween(s,e).stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }

    /*legacy - paging 없는 조회 (대시보드용)*/
    // storeId가 없는 경우
    @Transactional(readOnly = true)
    public List<PuduReportResponseDTO> getReportsAllStores(
            String startDate,
            String endDate
    ) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end   = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        return puduReportRepository
                .findByStartTimeBetween(start, end)
                .stream()
                .map(PuduReportResponseDTO::createReportResponseDTO)
                .toList();
    }

    // report에서 사용
    @Transactional(readOnly = true)
    public List<PuduReportResponseDTO> getReportByStore(
            Long storeId,
            String startDate,
            String endDate
    ) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end   = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        List<Long> robotIds = robotRepository.findRobotIdsByStoreId(storeId);

        if (robotIds.isEmpty()) {
            return List.of();
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "startTime");

        return puduReportRepository
                .findAllByRobot_RobotIdInAndStartTimeBetween(robotIds, start, end, sort)
                .stream()
                .map(PuduReportResponseDTO::createReportResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<PuduReportResponseDTO> getReportsPage(
            Long storeId,
            Long filterStoreId,
            String sn,
            LocalDateTime start,
            LocalDateTime end,
            int page,
            int size,
            String sortKey,
            String sortOrder
    ) {
        String resolvedSortKey = resolveSortKey(sortKey);

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortOrder)
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC,
                resolvedSortKey
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<PuduReport> result;

        if (sn != null && !sn.isBlank()) {
            result = puduReportRepository
                    .findByRobot_SnAndStartTimeBetween(
                            sn, start, end, pageable
                    );
        } else if (filterStoreId != null) {
            List<Long> robotIds = robotRepository.findRobotIdsByStoreId(filterStoreId);
            if (robotIds.isEmpty()) return Page.empty(pageable);

            result = puduReportRepository
                    .findAllByRobot_RobotIdInAndStartTimeBetween(
                            robotIds, start, end, pageable
                    );
        } else if (storeId != null) {
            List<Long> robotIds = robotRepository.findRobotIdsByStoreId(storeId);
            if (robotIds.isEmpty()) return Page.empty(pageable);

            result = puduReportRepository
                    .findAllByRobot_RobotIdInAndStartTimeBetween(
                            robotIds, start, end, pageable
                    );
        } else {
            result = puduReportRepository
                    .findByStartTimeBetween(start, end, pageable);
        }

        return result.map(PuduReportResponseDTO::createReportResponseDTO);
    }

    // ai report에서 사용
    public List<PuduReportDTO> getReportByStoreId(
            Long storeId,
            String startDate,
            String endDate
    ) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end   = LocalDate.parse(endDate).plusDays(1).atStartOfDay();

        return puduReportRepository
                .findByStoreIdAndPeriod(storeId, start, end)
                .stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }
}
