package com.codehows.taelimbe.pudureport.service;

import com.codehows.taelimbe.pudureport.dto.PuduReportDTO;
import com.codehows.taelimbe.pudureport.dto.StoreTimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.dto.TimeRangeSyncRequestDTO;
import com.codehows.taelimbe.pudureport.entity.PuduReport;
import com.codehows.taelimbe.pudureport.repository.PuduReportRepository;
import com.codehows.taelimbe.store.entity.Store;
import com.codehows.taelimbe.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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


    @Transactional
    public int syncSingleStoreByTimeRange(StoreTimeRangeSyncRequestDTO req){

        Store store = storeRepository.findById(req.getStoreId())
                .orElseThrow(()->new IllegalArgumentException("Store not found"));

        Long shopId = store.getShopId();

        int offset=req.getOffset(), saved=0;
        List<PuduReport> buffer=new ArrayList<>();

        while(true){

            List<Map<String,Object>> list = processor.fetchList(
                    req.getStartTime(),req.getEndTime(),
                    shopId,req.getTimezoneOffset(),offset);

            if(list.isEmpty()) break;

            List<CompletableFuture<PuduReport>> future = list.stream()
                    .map(x->processor.convertAsync(
                            (String)x.get("sn"),x.get("report_id").toString(),
                            req.getStartTime(),req.getEndTime(),
                            req.getTimezoneOffset(),shopId))
                    .toList();

            CompletableFuture.allOf(future.toArray(new CompletableFuture[0])).join();

            future.forEach(f->{
                PuduReport r=f.join();
                System.out.println(">>> REPORT: " + r); // ★ 로그 추가
                if(r!=null)buffer.add(r);});

            if(buffer.size()>=50){
                puduReportRepository.saveAll(buffer);
                saved+=buffer.size(); buffer.clear();
            }

            offset+=20;
        }

        if(!buffer.isEmpty()){
            puduReportRepository.saveAll(buffer);
            saved+=buffer.size();
        }

        return saved;
    }

    /* 전체 180일 */
    @Transactional
    public int syncSingleStoreFullHistorical(Long storeId){
        return syncSingleStoreByTimeRange(
                StoreTimeRangeSyncRequestDTO.builder()
                        .storeId(storeId)
                        .startTime(LocalDate.now().minusDays(180).atStartOfDay())
                        .endTime(LocalDate.now().atTime(LocalTime.MAX))
                        .timezoneOffset(0)
                        .offset(0).build()
        );
    }

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


    @Transactional
    public int syncAllStoresFullHistorical(){
        return storeRepository.findAll().stream()
                .mapToInt(s -> syncSingleStoreFullHistorical(s.getStoreId()))
                .sum();
    }





    public List<PuduReportDTO> getAllReports(){
        return puduReportRepository.findAll().stream()
                .map(PuduReportDTO::createReportDTO)
                .toList();
    }

    public PuduReportDTO getReportById(Long id){
        return puduReportRepository.findById(id)
                .map(PuduReportDTO::createReportDTO)
                .orElseThrow(() -> new IllegalArgumentException("Report not found: "+id));
    }

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

}
