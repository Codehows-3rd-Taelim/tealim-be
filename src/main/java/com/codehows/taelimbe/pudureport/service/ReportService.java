package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.pudureport.dto.*;
import com.codehows.taelimbe.pudureport.entity.Report;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.pudureport.repository.ReportRepository;
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

        Report report = Report.builder()
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

        return toDto(reportRepository.save(report));
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
