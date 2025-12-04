# pudureport

## src/main/java/com/codehows/taelimbe/pudureport/controller/ReportController.java
```java
package com.codehows.taelimbe.pudureport.controller;

import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.service.PuduReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/puduReport")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/sync/store/time-range")
    public ResponseEntity<String> syncSingleStoreByTimeRange(
            @Valid @RequestBody StoreTimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncSingleStoreByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료");
    }

    @PostMapping("/sync/store/full-historical")
    public ResponseEntity<String> syncSingleStoreFullHistorical(
            @Valid @RequestBody StoreFullHistoricalSyncRequestDTO req
    ) {
        int count = reportService.syncSingleStoreFullHistorical(req.getStoreId());
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (과거 185일)");
    }

    @PostMapping("/sync/all-stores/time-range")
    public ResponseEntity<String> syncAllStoresByTimeRange(
            @Valid @RequestBody TimeRangeSyncRequestDTO req
    ) {
        int count = reportService.syncAllStoresByTimeRange(req);
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 특정 기간)");
    }

    @PostMapping("/sync/all-stores/full-historical")
    public ResponseEntity<String> syncAllStoresFullHistorical() {
        int count = reportService.syncAllStoresFullHistorical();
        return ResponseEntity.ok(count + "개 Report 저장/업데이트 완료 (모든 매장 - 전체 기간)");
    }

    @GetMapping("/list/all")
    public ResponseEntity<List<ReportDTO>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<ReportDTO> getReportById(@PathVariable Long id) {
        return ResponseEntity.ok(reportService.getReportById(id));
    }

    @GetMapping("/list/robot/{sn}")
    public ResponseEntity<List<ReportDTO>> getReportsByRobotSn(@PathVariable String sn) {
        return ResponseEntity.ok(reportService.getReportsByRobotSn(sn));
    }
}
```

## src/main/java/com/codehows/taelimbe/pudureport/dto/ReportDetailRequestDTO.java
```java
package com.codehows.taelimbe.pudureport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

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
```

## src/main/java/com/codehows/taelimbe/pudureport/dto/ReportDTO.java
```java
package com.codehows.taelimbe.pudureport.dto;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDTO {

    private Long puduReportId;
    private Long reportId;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private Float cleanTime;
    private Float taskArea;
    private Float cleanArea;

    private Integer mode;
    private Long costBattery;
    private Long costWater;

    private String mapName;
    private String mapUrl;

    private Long robotId;

    public static ReportDTO createReportDTO(Report puduReport) {
        return ReportDTO.builder()
                .puduReportId(puduReport.getPuduReportId())
                .reportId(puduReport.getReportId())
                .status(puduReport.getStatus())
                .startTime(puduReport.getStartTime())
                .endTime(puduReport.getEndTime())
                .cleanTime(puduReport.getCleanTime())
                .taskArea(puduReport.getTaskArea())
                .cleanArea(puduReport.getCleanArea())
                .mode(puduReport.getMode())
                .costBattery(puduReport.getCostBattery())
                .costWater(puduReport.getCostWater())
                .mapName(puduReport.getMapName())
                .mapUrl(puduReport.getMapUrl())
                .robotId(puduReport.getRobot().getRobotId())
                .build();
    }
}
```

## src/main/java/com/codehows/taelimbe/pudureport/dto/StoreFullHistoricalSyncRequestDTO.java
```java
package com.codehows.taelimbe.pudureport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreFullHistoricalSyncRequestDTO {

    @NotNull(message = "storeId는 필수입니다")
    @Positive(message = "storeId는 양수여야 합니다")
    private Long storeId;
}
```

## src/main/java/com/codehows/taelimbe/pudureport/dto/StoreTimeRangeSyncRequestDTO.java
```java
package com.codehows.taelimbe.pudureport.dto;

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

    @AssertTrue(message = "startTime이 endTime보다 작아야 합니다")
    public boolean isValidTimeRange() {
        return startTime.isBefore(endTime);
    }
}
```

## src/main/java/com/codehows/taelimbe/pudureport/dto/TimeRangeSyncRequestDTO.java
```java
package com.codehows.taelimbe.pudureport.dto;

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
```

## src/main/java/com/codehows/taelimbe/pudureport/entity/Report.java
```java
package com.codehows.taelimbe.pudureport.entity;

import com.codehows.taelimbe.robot.entity.Robot;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "puduReport")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pudu_report_id")
    private Long puduReportId;

    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "status")
    private Integer status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "clean_time")
    private Float cleanTime;

    @Column(name = "task_area")
    private Float taskArea;

    @Column(name = "clean_area")
    private Float cleanArea;

    @Column(name = "mode")
    private Integer mode;

    @Column(name = "cost_battery")
    private Long costBattery;

    @Column(name = "cost_water")
    private Long costWater;

    @Column(name = "map_name", length = 255)
    private String mapName;

    @Column(name = "map_url", length = 255)
    private String mapUrl;

    @ManyToOne
    @JoinColumn(name = "robot_id")
    private Robot robot;

}
```

## src/main/java/com/codehows/taelimbe/pudureport/repository/ReportRepository.java
```java
package com.codehows.taelimbe.pudureport.repository;

import com.codehows.taelimbe.pudureport.entity.PuduReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByRobot_Sn(String sn);

    List<Report> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Report> findByReportId(Long reportId);
}
```

## src/main/java/com/codehows/taelimbe/pudureport/service/ReportService.java
```java
package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.codehows.taelimbe.store.repository.StoreRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PuduAPIClient puduAPIClient;
    private final ReportRepository reportRepository;
    private final RobotRepository robotRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ReportDTO> getReport(String startDate, String endDate) {

        if (startDate == null || startDate.isEmpty()) {
            startDate = LocalDate.now().minusWeeks(1).toString();
        }
        if (endDate == null || endDate.isEmpty()) {
            endDate = LocalDate.now().toString();
        }

        LocalDateTime startDateTime = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        return reportRepository.findByStartTimeBetween(startDateTime, endDateTime)
                .stream()
                .map(ReportDTO::createReportDTO)
                .toList();
    }


    @Transactional
    public int syncSingleStoreByTimeRange(StoreTimeRangeSyncRequestDTO req) {
        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();
        int totalCount=0, offset=req.getOffset();

        while(true){
            List<Map<String,Object>> list = fetchReportList(
                    req.getStartTime(), req.getEndTime(), shopId,
                    req.getTimezoneOffset(), offset
            );
            if(list.isEmpty()) break;

            for(Map<String,Object> item : list){
                if(saveReportDetailWithConversion(
                        getString(item,"sn"),
                        getString(item,"report_id"),
                        req.getStartTime(), req.getEndTime(),
                        req.getTimezoneOffset(), shopId
                )!=null) totalCount++;
            }
            offset+=20;
        }
        return totalCount;
    }



    @Transactional
    public int syncSingleStoreFullHistorical(Long storeId){
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found"));

        Long shopId=store.getShopId();
        LocalDateTime end = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime start = LocalDate.now().minusDays(180).atStartOfDay();

        int total=0,offset=0;

        while(true){
            List<Map<String,Object>> list = fetchReportList(start,end,shopId,0,offset);
            if(list.isEmpty()) break;

            for(Map<String,Object> item:list){
                if(saveReportDetailWithConversion(
                        getString(item,"sn"), getString(item,"report_id"),
                        start,end,0,shopId)!=null) total++;
            }
            offset+=20;
        }
        return total;
    }



    @Transactional
    public int syncAllStoresByTimeRange(TimeRangeSyncRequestDTO req){
        int total=0;
        for(Store s : storeRepository.findAll()){
            total+=syncSingleStoreByTimeRange(
                    StoreTimeRangeSyncRequestDTO.builder()
                            .storeId(s.getStoreId())
                            .startTime(req.getStartTime())
                            .endTime(req.getEndTime())
                            .timezoneOffset(req.getTimezoneOffset())
                            .offset(0).build()
            );
        }
        return total;
    }



    @Transactional
    public int syncAllStoresFullHistorical(){
        int total=0;
        for(Store s : storeRepository.findAll())
            total+=syncSingleStoreFullHistorical(s.getStoreId());
        return total;
    }



    private List<Map<String,Object>> fetchReportList(
            LocalDateTime start,LocalDateTime end,Long shopId,int timezoneOffset,int offset
    ){
        try{
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time",start.atZone(ZoneId.systemDefault()).toEpochSecond())
                    .queryParam("end_time",end.atZone(ZoneId.systemDefault()).toEpochSecond())
                    .queryParam("shop_id",shopId)
                    .queryParam("offset",offset)
                    .queryParam("limit",20)
                    .queryParam("timezone_offset",timezoneOffset)
                    .toUriString();

            JsonNode list = mapper.readTree(puduAPIClient.callPuduAPI(url,"GET").getBody())
                    .path("data").path("list");

            return mapper.convertValue(list,
                    mapper.getTypeFactory().constructCollectionType(List.class,Map.class));

        }catch(Exception e){ return new ArrayList<>();}
    }


    private Map<String,Object> fetchReportDetail(
            String sn,String reportId,LocalDateTime start,LocalDateTime end,int timezoneOffset,Long shopId){
        try{
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn",sn)
                    .queryParam("report_id",reportId)
                    .queryParam("start_time",start.atZone(ZoneId.systemDefault()).toEpochSecond())
                    .queryParam("end_time",end.atZone(ZoneId.systemDefault()).toEpochSecond())
                    .queryParam("timezone_offset",timezoneOffset)
                    .queryParam("shop_id",shopId)
                    .toUriString();

            JsonNode data = mapper.readTree(puduAPIClient.callPuduAPI(url,"GET").getBody())
                    .path("data");

            return mapper.convertValue(data,Map.class);

        }catch(Exception e){ return null;}
    }


    private Map<String,String> extractFloorInfo(Map<String,Object> detail) {
        Map<String,String> result = new HashMap<>();

        try {
            Object raw = detail.get("floor_list");

            List<Map<String,Object>> list =
                    mapper.convertValue(raw, mapper.getTypeFactory()
                            .constructCollectionType(List.class, Map.class));   // ← 안전 & 경고 없음

            if (!list.isEmpty()) {
                Map<String,Object> first = list.getFirst();

                result.put("mapName", getString(first,"map_name"));
                result.put("mapUrl", Optional.ofNullable(getString(first,"task_result_url"))
                        .orElse(getString(first,"task_local_url")));
            }

        } catch (Exception e) {
            System.out.println("floor_list parsing failed: "+e.getMessage());
        }
        return result;
    }



    private ReportDTO saveReportDetailWithConversion(
            String sn,String reportIdStr,LocalDateTime start,LocalDateTime end,int timezoneOffset,Long shopId
    ){
        Robot robot = robotRepository.findBySn(sn)
                .orElseThrow(() -> new IllegalArgumentException("Robot not found"));

        Long reportId = Long.parseLong(reportIdStr);
        if(reportRepository.findByReportId(reportId).isPresent()) return null;

        Map<String,Object> detail = fetchReportDetail(sn, reportIdStr, start,end,timezoneOffset,shopId);
        if(detail==null) return null;

        Map<String,String> floor = extractFloorInfo(detail);

        Report puduReport = Report.builder()
                .reportId(reportId)
                .status(getInt(detail,"status"))
                .startTime(toLocalDateTime(getLong(detail,"start_time")))
                .endTime(toLocalDateTime(getLong(detail,"end_time")))
                .cleanTime(getFloat(detail,"clean_time"))
                .taskArea(getFloat(detail,"task_area"))
                .cleanArea(getFloat(detail,"clean_area"))
                .mode(getInt(detail,"mode"))
                .costBattery(getLong(detail,"cost_battery"))
                .costWater(getLong(detail,"cost_water"))
                .mapName(floor.get("mapName"))
                .mapUrl(floor.get("mapUrl"))
                .robot(robot)
                .build();

        return toDto(reportRepository.save(puduReport));
    }

    public List<ReportDTO> getAllReports() {
        return reportRepository.findAll().stream().map(this::toDto).toList();
    }


    public ReportDTO getReportById(Long id) {
        return toDto(reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found")));
    }


    public List<ReportDTO> getReportsByRobotSn(String sn) {
        return reportRepository.findByRobot_Sn(sn)
                .stream().map(this::toDto).toList();
    }



    // UTC 타임스탬프 -> 로컬 시간
    private LocalDateTime toLocalDateTime(Long epoch){
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch),ZoneId.systemDefault());
    }


    private ReportDTO toDto(Report r){
        return ReportDTO.builder()
                .puduReportId(r.getPuduReportId())
                .reportId(r.getReportId())
                .status(r.getStatus())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .cleanTime(r.getCleanTime())
                .taskArea(r.getTaskArea())
                .cleanArea(r.getCleanArea())
                .mode(r.getMode())
                .costBattery(r.getCostBattery())
                .costWater(r.getCostWater())
                .mapName(r.getMapName())
                .mapUrl(r.getMapUrl())
                .robotId(r.getRobot().getRobotId())
                .build();
    }

    private String getString(Map<String,Object> map,String key){
        Object v = map.get(key);
        return v!=null ? v.toString() : null;
    }
    private Integer getInt(Map<String,Object> map,String key){
        return map.get(key) instanceof Number ? ((Number)map.get(key)).intValue() : null;
    }
    private Long getLong(Map<String,Object> map,String key){
        return map.get(key) instanceof Number ? ((Number)map.get(key)).longValue() : null;
    }
    private Float getFloat(Map<String,Object> map,String key){
        return map.get(key) instanceof Number ? ((Number)map.get(key)).floatValue() : null;
    }
}
```
