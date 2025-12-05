package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.codehows.taelimbe.client.PuduAPIClient;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.robot.entity.Robot;
import com.codehows.taelimbe.robot.repository.RobotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class PuduReportAsyncProcessor {

    private final PuduAPIClient puduAPIClient;
    private final PuduReportRepository puduReportRepository;
    private final RobotRepository robotRepository;
    private final ObjectMapper mapper = new ObjectMapper();


    public List<Map<String,Object>> fetchList(LocalDateTime s, LocalDateTime e,
                                              Long shopId,int tz,int offset){
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time", epoch(s))
                    .queryParam("end_time", epoch(e))
                    .queryParam("shop_id", shopId)
                    .queryParam("offset", offset)
                    .queryParam("limit", 20)
                    .queryParam("timezone_offset", tz)
                    .toUriString();

            JsonNode node = mapper.readTree(
                    puduAPIClient.callPuduAPI(url,"GET").getBody()
            ).path("data").path("list");

            return mapper.convertValue(node,
                    mapper.getTypeFactory().constructCollectionType(List.class,Map.class));

        }catch(Exception ex){ return List.of(); }
    }


    @Async("PuduReportSyncExecutor")
    public CompletableFuture<PuduReport> convertAsync(
            String sn,String reportIdStr,
            LocalDateTime start,LocalDateTime end,
            int tz,Long shopId) {

        try {
            Long reportId = safeLong(reportIdStr);
            if(reportId==null || puduReportRepository.findByReportId(reportId).isPresent())
                return done();

            Robot robot = robotRepository.findBySn(sn).orElse(null);
            if(robot==null) return done();

            Map<String,Object> data = fetchDetail(sn,reportIdStr,start,end,tz,shopId);
            if(data==null) return done();


            Map<String,String> floor = new HashMap<>();

            try {
                String floorStr = (String) data.get("floor_list"); // JSON string
                if(floorStr != null) {
                    List<Map<String,Object>> list = mapper.readValue(
                            floorStr,
                            mapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                    );

                    if(!list.isEmpty()) {
                        Map<String,Object> f = list.get(0);
                        floor.put("mapName", (String) f.get("map_name"));
                        floor.put("mapUrl", (String) Optional.ofNullable(f.get("task_result_url"))
                                .orElse(f.get("task_local_url")));
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }


            return CompletableFuture.completedFuture(
                    PuduReport.builder()
                            .reportId(reportId)
                            .status(i(data,"status"))
                            .startTime(ld(l(data,"start_time")))
                            .endTime(ld(l(data,"end_time")))
                            .cleanTime(f(data,"clean_time"))
                            .taskArea(f(data,"task_area"))
                            .cleanArea(f(data,"clean_area"))     // ← 이 라인 그대로!
                            .mode(i(data,"mode"))
                            .costBattery(l(data,"cost_battery"))
                            .costWater(l(data,"cost_water"))
                            .mapName(floor.get("mapName"))
                            .mapUrl(floor.get("mapUrl"))
                            .robot(robot)
                            .build()
            );


        } catch(Exception e){
            e.printStackTrace();
            return done();
        }
    }


    private Map<String,Object> fetchDetail(
            String sn,String id,LocalDateTime s,LocalDateTime e,int tz,Long shopId){
        try {
            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn", sn)
                    .queryParam("report_id", id)
                    .queryParam("start_time", epoch(s))
                    .queryParam("end_time", epoch(e))
                    .queryParam("timezone_offset", tz)
                    .queryParam("shop_id", shopId)
                    .toUriString();

            JsonNode node = mapper.readTree(
                    puduAPIClient.callPuduAPI(url,"GET").getBody()
            ).path("data");

            return mapper.convertValue(
                    node,
                    new TypeReference<Map<String, Object>>() {}
            );

        }catch(Exception ex){ return null; }
    }



    private static CompletableFuture<PuduReport> done(){return CompletableFuture.completedFuture(null);}
    private static Integer i(Map<String,Object> m,String k){return m.get(k)instanceof Number?((Number)m.get(k)).intValue():null;}
    private static Long l(Map<String,Object> m,String k){return m.get(k)instanceof Number?((Number)m.get(k)).longValue():null;}
    private static Float f(Map<String,Object> m,String k){return m.get(k)instanceof Number?((Number)m.get(k)).floatValue():null;}

    private static LocalDateTime ld(Long e){return e==null?null:LocalDateTime.ofInstant(Instant.ofEpochSecond(e),ZoneId.systemDefault());}
    private static Long epoch(LocalDateTime t){return t.atZone(ZoneId.systemDefault()).toEpochSecond();}
    private static Long safeLong(String s){try{return Long.parseLong(s);}catch(Exception e){return null;}}

    private Map<String,String> floor(Map<String,Object> d){
        try {
            List<Map<String,Object>> list = mapper.convertValue(
                    d.get("floor_list"), mapper.getTypeFactory().constructCollectionType(List.class,Map.class));

            if(!list.isEmpty()){
                Map<String,Object> f=list.get(0);
                return Map.of(
                        "mapName", (String) f.get("map_name"),
                        "mapUrl", Optional.ofNullable((String)f.get("task_result_url"))
                                .orElse((String)f.get("task_local_url"))
                );
            }
        }catch(Exception ignore){}
        return Map.of("mapName",null,"mapUrl",null);
    }
}
