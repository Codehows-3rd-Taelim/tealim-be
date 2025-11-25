package com.codehows.taelimbe.service;

import com.codehows.taelimbe.client.PuduAPIClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

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

            StringBuilder urlBuilder = new StringBuilder(
                    PuduAPIClient.BASE_URL + "/data-board/v1/log/clean_task/query_list?"
            );

            urlBuilder.append("start_time=").append(start_time);
            urlBuilder.append("&end_time=").append(end_time);
            urlBuilder.append("&offset=").append(offset);
            urlBuilder.append("&limit=").append(limit);
            urlBuilder.append("&timezone_offset=").append(timezone_offset);
            urlBuilder.append("&shop_id=").append(shop_id);

            String url = urlBuilder.toString();
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

            StringBuilder urlBuilder = new StringBuilder(
                    PuduAPIClient.BASE_URL + "/data-board/v1/log/clean_task/query?"
            );

            urlBuilder.append("sn=").append(sn);
            urlBuilder.append("&report_id=").append(report_id);
            urlBuilder.append("&start_time=").append(start_time);
            urlBuilder.append("&end_time=").append(end_time);
            urlBuilder.append("&timezone_offset=").append(timezone_offset);

            if (shop_id != null) {
                urlBuilder.append("&shop_id=").append(shop_id);
            }

            String url = urlBuilder.toString();
            System.out.println("Target Detail URL: " + url);

            return puduAPIClient.callPuduAPI(url, "GET");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
