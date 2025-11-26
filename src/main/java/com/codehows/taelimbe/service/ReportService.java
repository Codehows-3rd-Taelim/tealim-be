package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
public class ReportService {

    private final PuduAPIClient puduAPIClient;

    public ReportService(PuduAPIClient puduAPIClient) {
        this.puduAPIClient = puduAPIClient;
    }

    public ResponseEntity<String> getCleanReportList(long start_time, long end_time,
                                                     int offset, int limit, int timezone_offset,
                                                     Long shop_id) {
        try {
            System.out.println("====== Cleaning Report 목록 조회 시작 ======");

            String url = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query_list")
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("offset", offset)
                    .queryParam("limit", limit)
                    .queryParam("timezone_offset", timezone_offset)
                    .queryParam("shop_id", shop_id)
                    .toUriString();

            System.out.println("Target URL: " + url);
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    public ResponseEntity<String> getCleanReportDetail(String sn, String report_id,
                                                       long start_time, long end_time,
                                                       int timezone_offset, Long shop_id) {
        try {
            System.out.println("====== Cleaning Report 상세 조회 시작 ======");

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(puduAPIClient.getBaseUrl())
                    .path("/data-board/v1/log/clean_task/query")
                    .queryParam("sn", sn)
                    .queryParam("report_id", report_id)
                    .queryParam("start_time", start_time)
                    .queryParam("end_time", end_time)
                    .queryParam("timezone_offset", timezone_offset);

            if (shop_id != null) {
                builder.queryParam("shop_id", shop_id);
            }

            String url = builder.toUriString();
            System.out.println("Target Detail URL: " + url);
            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}